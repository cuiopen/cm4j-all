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
* Date: ${data?string("yyyy-MM-dd HH:mm:ss")}
*/
public class ${file_name} extends CacheDefiniens<MapReference<${map_key}, ${pojo}>> {
    public ${file_name}() {
    }

    public ${file_name}(int playerId) {
        super(playerId);
    }

    @Override
    public MapReference<${map_key}, ${pojo}> load(String... params) {
        Preconditions.checkArgument(params.length == 1);
        HibernateDao<${pojo}, ${map_key}> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        hibernate.setPersistentClass(${pojo}.class);
        String hql = "from ${pojo} where id.NPlayerId = ?";
        List<${pojo}> all = hibernate.findAll(hql, NumberUtils.toInt(params[0]));

        Map<${map_key}, ${pojo}> map = new HashMap<${map_key}, ${pojo}>();
        for (${pojo} entity : all) {
            map.put(entity.getId().getNType(), entity);
        }
        return new MapReference<${map_key}, ${pojo}>(map);
    }
}
