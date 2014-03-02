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
* Date: ${data?string("yyyy-MM-dd HH:mm:ss")}
 */
public class ${file_name} extends CacheDefiniens<ListReference<${pojo}>> {
    public ${file_name}() {
    }

    public ${file_name}(int playerId) {
        super(playerId);
    }

    @Override
    public ListReference<${pojo}> load(String... params) {
        Preconditions.checkArgument(params.length == 1);
        HibernateDao<${pojo}, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        hibernate.setPersistentClass(${pojo}.class);
        String hql = "from ${pojo} where id.NPlayerId = ?";
        return new ListReference<${pojo}>(hibernate.findAll(hql, NumberUtils.toInt(params[0])));
    }
}
