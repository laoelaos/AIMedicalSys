package com.aimedical.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SimpleMessageInterpolator implements MessageInterpolator {

    private static final Logger log = LoggerFactory.getLogger(SimpleMessageInterpolator.class);
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
                log.warn("MessageFormat 参数非法，返回原模板: template={}, err={}", template, e.getMessage());
            } catch (Exception e) {
                log.warn("MessageFormat 格式化异常，返回原模板: template={}", template, e);
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
