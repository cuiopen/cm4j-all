package net.bojoy.king.game.dict;

import com.google.common.collect.Maps;
import net.bojoy.king.core.common.IExtReload;
import net.bojoy.king.dao.webgame.dao.IQueryDictDAO;
import net.bojoy.king.dao.webgame.entity.${entityName};
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

/**
 * ${comment!""}
 *
 * @desc     代码生成器生成.
 * @date     ${.now?string("yyyy-MM-dd")}
 */
public class ${clsName} implements IExtReload {

    private static final Log log = LogFactory.getLog(DanYaoDictCache.class);
    private static final Map<${idType}, ${entityName}> data = Maps.newHashMap();

    private IQueryDictDAO queryDictDAO;

    @Override
    public void reload() {
        try {
            data.clear();
            List<${entityName}> all = queryDictDAO.findAll(${entityName}.class);
            for (${entityName} entity : all) {
                data.put(entity.${idGetterName}(), entity);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public static Map<${idType}, ${entityName}> getData() {
        return data;
    }

    public void setQueryDictDAO(IQueryDictDAO queryDictDAO) {
        this.queryDictDAO = queryDictDAO;
    }
}
