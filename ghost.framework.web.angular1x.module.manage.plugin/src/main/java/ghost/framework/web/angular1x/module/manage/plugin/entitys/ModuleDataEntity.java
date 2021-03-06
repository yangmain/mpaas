package ghost.framework.web.angular1x.module.manage.plugin.entitys;

import ghost.framework.beans.annotation.application.Application;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * package: ghost.framework.web.angular1x.module.manage.plugin.entitys
 *
 * @Author: 郭树灿{gsc-e590}
 * @link: 手机:13715848993, QQ 27048384
 * @Description:模块数据表
 * @Date: 2020/4/22:19:38
 */
@Application
@Entity
@Table(name= "module_data",
        indexes = {
                @Index(name = "uk", columnList = "module_id", unique = true),
                @Index(name = "pk", columnList = "module_id,size")
        })
@Cacheable(value = false)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONE)
public class ModuleDataEntity implements Serializable {
    private static final long serialVersionUID = 1915452028444536005L;
    /**
     * 模块id
     */
    @Id
    @Column(name = "module_id", updatable = false, nullable = false, length = 36, columnDefinition = "char(36)")
    @NotNull(message = "null error")
    @NotEmpty(message = "empty error")
    @Length(max = 36, min = 36, message = "length error")
    private String moduleId;

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleId() {
        return moduleId;
    }

    /**
     * jar包大小
     */
    @Column(name = "size", updatable = false, nullable = false)
    @NotNull(message = "null error")
    @Min(1)
    private int size;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * jar包数据
     */
    @Lob
    @Basic(fetch=FetchType.LAZY)
//    @Column(name = "data", updatable = false, nullable = false, columnDefinition="BLOB")
    @Column(name = "data", updatable = false, nullable = false)
    @NotNull(message = "null error")
    private byte[] data;

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}