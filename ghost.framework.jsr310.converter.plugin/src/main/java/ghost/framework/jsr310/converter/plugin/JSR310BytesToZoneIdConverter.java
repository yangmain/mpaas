package ghost.framework.jsr310.converter.plugin;

import com.google.common.base.Objects;
import ghost.framework.beans.annotation.converter.ConverterFactory;
import ghost.framework.beans.annotation.injection.Autowired;
import ghost.framework.context.base.ICoreInterface;
import ghost.framework.context.converter.ConverterException;
import ghost.framework.jsr310.converter.BytesToZoneIdConverter;
import ghost.framework.jsr310.converter.Jsr310Converter;
import ghost.framework.util.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;

/**
 * package: ghost.framework.jsr310.convert.plugin
 *
 * @Author: 郭树灿{gsc-e590}
 * @link: 手机:13715848993, QQ 27048384
 * @Description: {@link byte[]} to {@link ZoneId}
 * @Date: 2020/6/26:1:23
 */
@ConverterFactory
public class JSR310BytesToZoneIdConverter implements BytesToZoneIdConverter<byte[], ZoneId> {

    public JSR310BytesToZoneIdConverter(@Autowired(required = false) ICoreInterface domain) {
        this.domain = domain;
    }

    private Object domain;

    @Override
    public Object getDomain() {
        return domain;
    }

    private final Class<?>[] sourceType = {byte[].class};

    @Override
    public Class<?>[] getSourceType() {
        return sourceType;
    }

    private final Class<?>[] targetType = {Duration.class};

    @Override
    public Class<?>[] getTargetType() {
        return targetType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSR310BytesToZoneIdConverter that = (JSR310BytesToZoneIdConverter) o;
        return Objects.equal(domain, that.domain) &&
                Objects.equal(sourceType, that.sourceType) &&
                Objects.equal(targetType, that.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(domain, sourceType, targetType);
    }

    @Override
    public String toString() {
        return "JSR310BytesToZoneIdConverter{" +
                "domain=" + (domain == null ? "" : domain.toString()) +
                ", sourceType=" + Arrays.toString(sourceType) +
                ", targetType=" + Arrays.toString(targetType) +
                '}';
    }

    @Override
    public boolean canConvert(byte[] source) {
        try {
            //判断是否可以转换
            if (CollectionUtils.isEmpty(source)) {
                return false;
            }
            return true;
        } catch (IllegalArgumentException e) {
        }
        return false;
    }

    @Override
    public ZoneId convert(byte[] source, String characterEncoding) throws ConverterException {
        try {
            return ZoneId.of(Jsr310Converter.StringBasedConverter.toString(source, characterEncoding));
        } catch (UnsupportedEncodingException e) {
            throw new ConverterException(e.getMessage(), e);
        }
    }

    @Override
    public ZoneId convert(byte[] source) throws ConverterException {
        return ZoneId.of(Jsr310Converter.StringBasedConverter.toString(source));
    }
}