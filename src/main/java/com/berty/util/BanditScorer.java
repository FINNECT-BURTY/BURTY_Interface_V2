package com.berty.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

/**
 * ε-greedy bandit chooser.
 * - With probability ε, picks a random candidate (explore).
 * - Otherwise picks the highest-scored candidate (exploit).
 * Score function and exploration rate are caller-controlled.
 */
@Component
public class BanditScorer {

    private final double explorationEpsilon;
    private final Random random;

    public BanditScorer(@Value("${berty.bandit.epsilon:0.10}") double explorationEpsilon) {
        this.explorationEpsilon = clamp(explorationEpsilon);
        this.random = ThreadLocalRandom.current();
    }

    public <T> T chooseTop(List<T> candidates, ToDoubleFunction<T> score) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);
        if (random.nextDouble() < explorationEpsilon) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        T best = candidates.get(0);
        double bestScore = score.applyAsDouble(best);
        for (int i = 1; i < candidates.size(); i++) {
            T c = candidates.get(i);
            double s = score.applyAsDouble(c);
            if (s > bestScore) {
                best = c;
                bestScore = s;
            }
        }
        return best;
    }

    public double explorationEpsilon() {
        return explorationEpsilon;
    }

    private double clamp(double v) {
        if (v < 0.0) return 0.0;
        if (v > 0.5) return 0.5;
        return v;
    }
}
