package ghost.framework.context.bean.factory;


/**
 * package: ghost.framework.core.bean.factory.injection.factory
 *
 * @Author: 郭树灿{gsc-e590}
 * @link: 手机:13715848993, QQ 27048384
 * @Description:值事件目标处理接口
 * @Date: 2020/1/8:8:52
 */
public interface IValueBeanTargetHandle<O, T, V> extends IExecuteOwnerBeanTargetHandle<O, T> {
    /**
     * 获取值
     * @return
     */
    V getValue();

    /**
     * 设置值
     * @param value
     */
    void setValue(V value);
}