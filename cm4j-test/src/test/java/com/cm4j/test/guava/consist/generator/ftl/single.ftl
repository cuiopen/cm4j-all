package ${package};

import org.apache.commons.lang.math.NumberUtils;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.SingleReference;
import com.cm4j.test.guava.consist.entity.${pojo};
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

/**
* ${comment!'COMMENT HERE'}
*
* User: ${author!'AUTHOR'}
* Date: ${data?string("yyyy-MM-dd HH:mm:ss")}
*/
public class ${file_name} extends CacheDefiniens<SingleReference<${pojo}>> {

    public ${file_name}() {
    }

    public ${file_name}(${constructor_params}) {
        super(${constructor_values});
    }

    @Override
    public SingleReference<${pojo}> load(String... params) {
        Preconditions.checkArgument(params.length == 1);
        HibernateDao<${pojo}, ${hibernate_key}> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        hibernate.setPersistentClass(${pojo}.class);
        return new SingleReference<${pojo}>(hibernate.findById(NumberUtils.toInt(params[0])));
    }
}
