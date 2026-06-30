package com.aimedical.common.util;

import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SimpleMessageInterpolator implements MessageInterpolator {

    private static final Pattern NAMED_PLACEHOLDER = Pattern.compile("\\{[^}]+\\}");
    private static final Pattern INDEXED_PLACEHOLDER = Pattern.compile(".*\\{\\d+\\}.*");

    @Override
    public String interpolate(String template, Object[] args) {
        if (template == null) {
            return null;
        }
        if (args == null || args.length == 0) {
            return template;
        }
        if (INDEXED_PLACEHOLDER.matcher(template).matches()) {
            try {
                return MessageFormat.format(template, args);
            } catch (IllegalArgumentException e) {
                // MessageFormat 格式不匹配时降级到命名占位符处理
            } catch (Exception e) {
                return template;
            }
        }
        String result = template;
        Matcher matcher = NAMED_PLACEHOLDER.matcher(result);
        for (Object arg : args) {
            if (!matcher.find()) {
                break;
            }
            String replacement = Matcher.quoteReplacement(String.valueOf(arg));
            result = matcher.replaceFirst(replacement);
            matcher = NAMED_PLACEHOLDER.matcher(result);
        }
        return result;
    }
}
