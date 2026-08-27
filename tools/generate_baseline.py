#!/usr/bin/env python3
"""build/generated-schema.sql (Hibernate 생성) → Flyway 신규환경 베이스라인 변환기.

변환 내용:
  1. create table            → create table if not exists
  2. unique / foreign key 제약 → CREATE TABLE 안으로 인라인
     (테이블이 이미 있는 기존 DB 에서는 문장 전체가 skip 되어 no-op 이 된다)
  3. enum ('A','B')          → varchar(N)
     네이티브 ENUM 은 상수 추가마다 ALTER 가 필요하다. JDBC 메타데이터상 어차피 VARCHAR 로
     보고되므로 ddl-auto=validate 와도 호환된다.
  4. create index            → create index if not exists

사용: ./gradlew test --tests com.burty.SchemaDumpTests  후  python3 tools/generate_baseline.py
"""
import re, sys, collections, pathlib

SRC = pathlib.Path('build/generated-schema.sql')
DST = pathlib.Path('src/main/resources/db/migration/V3__fresh_install_baseline.sql')

def enum_to_varchar(body: str) -> str:
    def repl(m):
        labels = re.findall(r"'([^']*)'", m.group(1))
        width = max([len(l) for l in labels] + [20])
        return 'varchar(%d)' % width
    return re.sub(r"enum \(((?:'[^']*',?\s*)+)\)", repl, body)

def main():
    if not SRC.exists():
        sys.exit('먼저 SchemaDumpTests 로 %s 를 생성하세요.' % SRC)
    stmts = [' '.join(s.split()) for s in SRC.read_text().split(';') if s.strip()]

    creates = collections.OrderedDict()
    uniques = collections.defaultdict(list)
    fks = collections.defaultdict(list)
    indexes = []
    unknown = []

    re_create = re.compile(r'^create table (\w+) \((.*)\) (engine=\w+)$', re.S)
    re_uk = re.compile(r'^alter table if exists (\w+)\s+add constraint \w+\s+unique \((.*)\)$', re.S)
    re_fk = re.compile(r'^alter table if exists (\w+)\s+add constraint \w+\s+foreign key \((.*?)\)\s+references (\w+) \((.*?)\)$', re.S)
    re_ix = re.compile(r'^create index (\w+)\s+on (\w+) \((.*)\)$', re.S)

    for s in stmts:
        if (m := re_create.match(s)):   creates[m.group(1)] = (m.group(2), m.group(3))
        elif (m := re_uk.match(s)):     uniques[m.group(1)].append(m.group(2))
        elif (m := re_fk.match(s)):     fks[m.group(1)].append((m.group(2), m.group(3), m.group(4)))
        elif (m := re_ix.match(s)):     indexes.append((m.group(1), m.group(2), m.group(3)))
        else:                           unknown.append(s)

    if unknown:
        sys.exit('변환기가 모르는 문장이 있습니다:\n  ' + '\n  '.join(unknown[:5]))

    out = ["""-- BURTY 스키마 부트스트랩 (신규 환경 전용) — 자동 생성 파일
--
-- 생성: ./gradlew test --tests com.burty.SchemaDumpTests && python3 tools/generate_baseline.py
--
-- 목적
--   ddl-auto=validate 환경에서 빈 데이터베이스가 부팅 가능하도록 전체 스키마를 만든다.
--   예전에는 V1 이 주석 한 줄뿐이라 신규 환경이 아예 뜨지 못했고, 스키마의 진짜 원본은
--   손으로 관리하는 db/burty_Table_Ver1.1.sql 이었다 (이미 낡아 있었다).
--
-- 안전성
--   모든 문장이 IF NOT EXISTS 이고 UNIQUE/FK 는 CREATE TABLE 안에 인라인되어 있다.
--   따라서 테이블이 이미 있는 기존 데이터베이스에서는 이 마이그레이션 전체가 no-op 이며,
--   기존 제약을 중복 생성하지 않는다.
--
-- 주의
--   이 파일을 손으로 고치지 말 것. 스키마 변경은 새 V 마이그레이션으로 추가한다.
--   (Flyway 는 적용된 마이그레이션의 체크섬을 검증하므로 수정 시 기존 환경이 깨진다.)

SET FOREIGN_KEY_CHECKS = 0;
"""]

    for table, (body, tail) in creates.items():
        parts = [enum_to_varchar(body)]
        for cols in uniques.get(table, []):
            parts.append('constraint uk_%s_%s unique (%s)'
                         % (table[4:] if table.startswith('tbl_') else table,
                            re.sub(r'\W+', '_', cols), cols))
        for cols, rt, rcols in fks.get(table, []):
            parts.append('constraint fk_%s_%s foreign key (%s) references %s (%s)'
                         % (table[4:] if table.startswith('tbl_') else table,
                            re.sub(r'\W+', '_', cols), cols, rt, rcols))
        out.append('create table if not exists %s (\n    %s\n) %s;\n' % (table, ',\n    '.join(parts), tail))

    out.append('\nSET FOREIGN_KEY_CHECKS = 1;\n')
    for name, table, cols in indexes:
        out.append('create index if not exists %s on %s (%s);' % (name, table, cols))

    DST.write_text('\n'.join(out) + '\n')
    print('tables=%d uniques=%d fks=%d indexes=%d → %s'
          % (len(creates), sum(map(len, uniques.values())), sum(map(len, fks.values())), len(indexes), DST))

main()
