package ${package};

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.MapReference;
import com.cm4j.test.guava.consist.entity.${pojo};
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.math.NumberUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* ${comment!'COMMENT HERE'}
*
* User: ${author!'AUTHOR'}
* Date: ${date_now?string("yyyy-MM-dd HH:mm:ss")}
*/
public class ${file_name} extends CacheDefiniens<MapReference<${map_key}, ${pojo}>> {
    public ${file_name}() {
    }

    public ${file_name}(${constructor_params}) {
        super(${constructor_values});
    }

    @Override
    public MapReference<${entry_key_type}, ${pojo}> load(String... params) {
        Preconditions.checkArgument(params.length == ${constructor_params_size});
        HibernateDao<${pojo}, ${pojo_id_type}> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        hibernate.setPersistentClass(${pojo}.class);
        List<${pojo}> all = ${hibernate_query};

        Map<${entry_key_type}, ${pojo}> map = new HashMap<${entry_key_type}, ${pojo}>();
        for (${pojo} entry : all) {
            map.put(${entry_key_content}, entity);
        }
        return new MapReference<${entry_key_type}, ${pojo}>(map);
    }
}
