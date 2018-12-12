package shuchaowen.db.id.factory;

import java.util.HashMap;
import java.util.Map;

import shuchaowen.db.ColumnInfo;
import shuchaowen.db.DB;
import shuchaowen.db.DBManager;
import shuchaowen.db.TableInfo;

public abstract class AbstractIdGeneratorFactory implements IdGeneratorFactory{
	private Map<Class<?>, MaxId> map = new HashMap<Class<?>, MaxId>();
	
	public void register(Class<?> tableClass, MaxId maxId){
		map.put(tableClass, maxId);
	}
	
	public long getMaxId(Class<?> tableClass){
		TableInfo tableInfo = DB.getTableInfo(tableClass);
		MaxId maxId = map.get(tableClass);
		if(maxId == null){
			ColumnInfo firstColumn = tableInfo.getColumns()[0];
			Long max = (Long) DBManager.getDB(tableClass).getMaxValue(firstColumn.getFieldInfo().getType(), tableClass, firstColumn.getName());
			return max == null? 0:max;
		}else{
			return maxId.get();
		}
	}
}