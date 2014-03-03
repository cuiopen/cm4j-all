package ${package};

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.ListReference;
import com.cm4j.test.guava.consist.entity.${pojo};
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.math.NumberUtils;

/**
 * ${comment!'COMMENT HERE'}
 *
* User: ${author!'AUTHOR'}
* Date: ${date_now?string("yyyy-MM-dd HH:mm:ss")}
 */
public class ${file_name} extends CacheDefiniens<ListReference<${pojo}>> {
    public ${file_name}() {
    }

    public ${file_name}(${constructor_params}) {
        super(${constructor_values});
    }

    @Override
    public ListReference<${pojo}> load(String... params) {
        Preconditions.checkArgument(params.length == ${constructor_params_size});
        HibernateDao<${pojo}, ${hibernate_key}> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        hibernate.setPersistentClass(${pojo}.class);
        return new ListReference<${pojo}>(${hibernate_query});
    }
}
