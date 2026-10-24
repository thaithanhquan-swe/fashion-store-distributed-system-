package com.fashionstore.backend.service;

import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import java.util.Collection;
import java.util.Locale;

final class GeneratedId {

    private GeneratedId() {
    }

    static String next(String prefix, Collection<String> existingIds, ErrorCode capacityError) {
        int highest = 0;
        for (String id : existingIds) {
            if (id == null || !id.startsWith(prefix)) {
                continue;
            }
            String suffix = id.substring(prefix.length());
            if (suffix.isEmpty() || suffix.length() > 8
                    || !suffix.chars().allMatch(ch -> ch >= '0' && ch <= '9')) {
                continue;
            }
            highest = Math.max(highest, Integer.parseInt(suffix));
        }
        if (highest >= 999) {
            throw new AppException(capacityError);
        }
        return String.format(Locale.ROOT, "%s%03d", prefix, highest + 1);
    }
}
