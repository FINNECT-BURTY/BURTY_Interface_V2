# 커밋 메시지와 GitHub 이슈 연결 규칙

## 커밋 메시지 형식

- 제목과 본문은 빈 줄로 구분
- 제목은 50자 이내 작성
- 제목 첫 글자는 대문자 시작, 마침표 미사용
- 제목은 명령문 사용, 과거형 미사용
- 본문 각 줄은 72자 이내 작성
- 본문은 **무엇**과 **왜** 중심 작성

예시:

`FEAT: 월간 리포트 응답 모델 반영`

`무엇: 리포트 응답 필드와 직렬화 정책 반영`
`왜: API 계약 일관성 유지 및 프론트 연동 안정성 확보`

## 타입 + gitmoji 매핑

- FEAT: ✨
- FIX: 🐛
- REFACTOR: ♻️
- CHORE: 🔧

## 이슈 연동 규칙

- 커밋 제목 끝에 이슈 번호 표기: `CHORE: 템플릿 정비 반영 (#12)`
- PR 본문에 `Closes #12` 또는 `Refs #12` 표기
- 이슈 생성 시 타입에 맞는 gitmoji 라벨 부여

## 저장소 이슈 링크

[FINNECT-BERTY/BERTY_Interface Issues](https://github.com/FINNECT-BERTY/BERTY_Interface/issues)
