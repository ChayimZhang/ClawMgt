package com.qwenpaw.clawmgt.task;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VersionComparator {
    public int compare(String left, String right) {
        List<String> leftParts = tokenize(left);
        List<String> rightParts = tokenize(right);
        int max = Math.max(leftParts.size(), rightParts.size());
        for (int i = 0; i < max; i++) {
            String leftPart = i < leftParts.size() ? leftParts.get(i) : "0";
            String rightPart = i < rightParts.size() ? rightParts.get(i) : "0";
            int comparison = comparePart(leftPart, rightPart);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private List<String> tokenize(String version) {
        String[] rawParts = version.split("[^A-Za-z0-9]+");
        List<String> parts = new ArrayList<>();
        for (String rawPart : rawParts) {
            if (!rawPart.isBlank()) {
                parts.add(rawPart);
            }
        }
        return parts;
    }

    private int comparePart(String left, String right) {
        boolean leftNumeric = left.chars().allMatch(Character::isDigit);
        boolean rightNumeric = right.chars().allMatch(Character::isDigit);
        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? 1 : -1;
        }
        return left.compareTo(right);
    }
}
