package cn.mcarl.miars.storage.storage;

import cc.carm.lib.easysql.EasySQL;
import cc.carm.lib.easysql.api.SQLManager;
import cn.mcarl.miars.storage.MiarsStorage;
import cn.mcarl.miars.storage.conf.PluginConfig;
import cn.mcarl.miars.storage.entity.*;
import cn.mcarl.miars.storage.enums.FKitType;
import cn.mcarl.miars.storage.utils.BukkitUtils;
import cn.mcarl.miars.storage.utils.DatabaseTable;
import com.alibaba.fastjson.JSONArray;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MySQLStorage {

	SQLManager sqlManager;

	DatabaseTable mPlayerDataTable;
	DatabaseTable mRankDataTable;

	DatabaseTable fPlayerDataTable;
	DatabaseTable fKitDataTable;

	private Gson gson = new Gson();

	public boolean initialize() {

		try {
			MiarsStorage.getInstance().log("	尝试连接到数据库...");
			this.sqlManager = EasySQL.createManager(PluginConfig.DATABASE.DRIVER_NAME.get(), PluginConfig.DATABASE.URL.get(), PluginConfig.DATABASE.USERNAME.get(), PluginConfig.DATABASE.PASSWORD.get());
			this.sqlManager.setDebugMode(false);
		} catch (Exception exception) {
			MiarsStorage.getInstance().log("无法连接到数据库，请检查配置文件。");
			exception.printStackTrace();
			return false;
		}

		try {
			MiarsStorage.getInstance().log("	创建插件所需表...");

			// 服务器用户信息
			this.mPlayerDataTable = new DatabaseTable(
					PluginConfig.DATABASE.TABLE_NAME.get()+"_mPlayer",
					new String[]{
							"`uuid` VARCHAR(36) NOT NULL", // 用户的UUID
							"`rank` VARCHAR(36) NOT NULL DEFAULT 'default'",// 当前所佩戴的头衔
							"`ranks` TEXT",// 当前所佩戴的头衔
							"`update_time` DATE", // 更新时间
							"`create_time` DATE", // 创建时间
							"PRIMARY KEY (`uuid`) USING BTREE"
					});
			getMPlayerTable().createTable(sqlManager);

			// 服务器头衔信息
			this.mRankDataTable = new DatabaseTable(
					PluginConfig.DATABASE.TABLE_NAME.get()+"_mRank",
					new String[]{
							"`id` int(11) NOT NULL AUTO_INCREMENT", // 头衔ID
							"`name` varchar(255) DEFAULT NULL",// 头衔名称
							"`prefix` varchar(255) DEFAULT NULL",// 前缀
							"`suffix` varchar(255) DEFAULT NULL",// 后缀
							"`nameColor` varchar(255) DEFAULT NULL",// 名字颜色
							"`permissions` varchar(255) DEFAULT NULL",// 权限
							"`group` varchar(255) DEFAULT NULL",// 权限组
							"`update_time` DATE", // 更新时间
							"`create_time` DATE", // 创建时间
							"PRIMARY KEY (`id`)", // 主键
					});
			getRankDataTable().createTable(sqlManager);

			// 竞技场玩家信息
			this.fPlayerDataTable = new DatabaseTable(
					PluginConfig.DATABASE.TABLE_NAME.get() + "_fPlayer",
					new String[]{
							"`uuid` varchar(255) NOT NULL",
							"`kills_count` bigint(20) DEFAULT '0'",
							"`death_count` bigint(20) DEFAULT '0'",
							"`rank_score` bigint(20) DEFAULT '0'",
							"`update_time` DATE", // 更新时间
							"`create_time` DATE", // 创建时间
							"PRIMARY KEY (`uuid`) USING BTREE"
					});
			getFPlayerDataTable().createTable(sqlManager);


			// 竞技场玩家套件信息
			this.fKitDataTable = new DatabaseTable(
					PluginConfig.DATABASE.TABLE_NAME.get() + "_fKit",
					new String[]{
							"`uuid` varchar(255) NOT NULL",
							"`type` varchar(255) DEFAULT NULL",
							"`name` varchar(255) DEFAULT NULL",
							"`inventory` longtext",
							"`power` int(11) DEFAULT NULL",
							"`update_time` datetime DEFAULT NULL",
							"`create_time` datetime DEFAULT NULL",
							"PRIMARY KEY (`uuid`) USING BTREE"
					});
			getFKitDataTable().createTable(sqlManager);

		} catch (SQLException exception) {
			MiarsStorage.getInstance().log("无法创建插件所需的表，请检查数据库权限。");
			exception.printStackTrace();
			return false;
		}

		return true;
	}

	public void shutdown() {
		MiarsStorage.getInstance().log("	关闭数据库连接...");
		EasySQL.shutdownManager(getSQLManager());
	}

	public void replaceMPlayer(@NotNull MPlayer data) throws Exception {
		getSQLManager().createReplace(getMPlayerTable().getTableName())
				.setColumnNames("uuid", "rank", "ranks", "update_time", "create_time")
				.setParams(data.getUuid(), data.getRank(), JSONArray.toJSON(data.getRanks()).toString(), data.getUpdateTime(),data.getCreateTime())
				.execute();
	}

	public MPlayer queryMPlayerByUUID(@NotNull String uuid) {
		return getSQLManager().createQuery()
				.inTable(getMPlayerTable().getTableName())
				.selectColumns("uuid", "rank", "ranks", "update_time", "create_time")
				.addCondition("uuid", uuid)
				.build()
				.execute(
						(query) -> {
							ResultSet result = query.getResultSet();
							MPlayer data = new MPlayer();
							if (result != null && result.next()) {
								data.setUuid(result.getString("uuid"));
								data.setRank(result.getString("rank"));
								data.setRanks(JSONArray.parseArray(result.getString("ranks")).toJavaList(String.class));
								data.setUpdateTime(result.getDate("update_time"));
								data.setCreateTime(result.getDate("create_time"));
								return data;
							}
							return null;
						},
						((exception, sqlAction) -> { /*SQL异常处理-SQLExceptionHandler*/ })
				);
	}

	/**
	 * 保存头衔数据
	 * @param data
	 * @throws Exception
	 */
	public void replaceMRankData(@NotNull MRank data) throws Exception {
		getSQLManager().createReplace(getRankDataTable().getTableName())
				.setColumnNames("name", "prefix", "suffix", "nameColor", "permissions", "group", "update_time", "create_time")
				.setParams(data.getName(), data.getPrefix(), data.getSuffix(), data.getNameColor(), data.getPermissions(), data.getGroup(), data.getUpdateTime(),data.getCreateTime())
				.execute();
	}

	/**
	 * 按名称查询头衔数据
	 * @param name
	 * @return
	 */
	public MRank queryMRankDataByName(@NotNull String name) {
		return getSQLManager().createQuery()
				.inTable(getRankDataTable().getTableName())
				.selectColumns("id", "name", "prefix", "suffix", "nameColor", "permissions", "group", "update_time", "create_time")
				.addCondition("name", name)
				.build()
				.execute(
						(query) -> {
							ResultSet result = query.getResultSet();
							MRank data = new MRank();
							if (result != null && result.next()) {
								data.setId(result.getInt("id"));
								data.setName(result.getString("name"));
								data.setPrefix(result.getString("prefix"));
								data.setSuffix(result.getString("suffix"));
								data.setNameColor(result.getString("nameColor"));
								data.setPermissions(result.getString("permissions"));
								data.setGroup(result.getString("group"));
								data.setUpdateTime(result.getDate("update_time"));
								data.setCreateTime(result.getDate("create_time"));
								return data;
							}
							return null;
						},
						((exception, sqlAction) -> { /*SQL异常处理-SQLExceptionHandler*/ })
				);
	}

	/**
	 * 保存用户信息
	 *
	 * @param data
	 * @throws Exception
	 */
	public void replaceFPlayerData(@NotNull FPlayer data) throws Exception {
		getSQLManager().createReplace(getFPlayerDataTable().getTableName())
				.setColumnNames("uuid", "kills_count", "death_count", "rank_score", "update_time", "create_time")
				.setParams(data.getUuid(), data.getKillsCount(), data.getDeathCount(), data.getRankScore(), data.getUpdateTime(), data.getCreateTime())
				.execute();
	}

	/**
	 * 根据UUID查询用户信息
	 *
	 * @param uuid
	 * @return
	 */
	public FPlayer queryFPlayerDataByUUID(@NotNull String uuid) {
		return getSQLManager().createQuery()
				.inTable(getFPlayerDataTable().getTableName())
				.selectColumns("uuid", "kills_count", "death_count", "rank_score", "update_time", "create_time")
				.addCondition("uuid", uuid)
				.build()
				.execute(
						(query) -> {
							ResultSet result = query.getResultSet();
							FPlayer data = new FPlayer();
							if (result != null && result.next()) {
								data.setUuid(result.getString("uuid"));
								data.setKillsCount(result.getLong("kills_count"));
								data.setDeathCount(result.getLong("death_count"));
								data.setRankScore(result.getLong("rank_score"));
								data.setUpdateTime(result.getDate("update_time"));
								data.setCreateTime(result.getDate("create_time"));
								return data;
							}
							return null;
						},
						((exception, sqlAction) -> { /*SQL异常处理-SQLExceptionHandler*/ })
				);
	}

	/**
	 * 更新或者插入套件数据
	 *
	 * @param data
	 * @throws Exception
	 */
	public void replaceFKitData(@NotNull FKit data) throws Exception {
		getSQLManager().createReplace(getFKitDataTable().getTableName())
				.setColumnNames("uuid", "type", "name", "inventory", "power", "update_time", "create_time")
				.setParams(data.getUuid(), data.getType().name(), data.getName(), gson.toJson(BukkitUtils.ItemStackConvertByte(data.getInventory())), data.getPower(), data.getUpdateTime(), data.getCreateTime())
				.execute();
	}

	/**
	 * 根据条件查询该玩家的套件
	 *
	 * @return
	 */
	public List<FKit> queryFKitDataList(@NotNull UUID uuid, FKitType type) {
		return getSQLManager().createQuery()
				.inTable(getFKitDataTable().getTableName())
				.selectColumns("uuid", "type", "name", "inventory", "power", "update_time", "create_time")
				.addCondition("uuid", uuid)
				.addCondition("type", type.name())
				.build()
				.execute(
						(query) -> {
							ResultSet result = query.getResultSet();
							List<FKit> datas = new ArrayList<>();

							while (result.next()) {
								FKit data = new FKit();
								data.setUuid(result.getString("uuid"));
								data.setType(FKitType.valueOf(result.getString("type")));
								data.setName(result.getString("name"));
								data.setInventory(BukkitUtils.byteConvertItemStack(gson.fromJson(result.getString("inventory"), FInventoryByte.class)));
								data.setPower(result.getInt("power"));
								data.setUpdateTime(result.getDate("update_time"));
								data.setCreateTime(result.getDate("create_time"));
								datas.add(data);
							}

							return datas;
						},
						((exception, sqlAction) -> { /*SQL异常处理-SQLExceptionHandler*/ })
				);
	}

	/**
	 * 查询数据库中所有的头衔数据
	 * @return
	 */
	public List<MRank> queryRankDataList() {
		return getSQLManager().createQuery()
				.inTable(getRankDataTable().getTableName())
				.selectColumns("id", "name", "prefix", "suffix", "nameColor", "permissions", "group", "update_time", "create_time")
				.build()
				.execute(
						(query) -> {
							ResultSet result = query.getResultSet();
							List<MRank> datas = new ArrayList<>();

							while (result.next()){
								MRank data = new MRank();
								data.setId(result.getInt("id"));
								data.setName(result.getString("name"));
								data.setPrefix(result.getString("prefix"));
								data.setSuffix(result.getString("suffix"));
								data.setNameColor(result.getString("nameColor"));
								data.setPermissions(result.getString("permissions"));
								data.setGroup(result.getString("group"));
								data.setUpdateTime(result.getDate("update_time"));
								data.setCreateTime(result.getDate("create_time"));
								datas.add(data);
							}

							return datas;
						},
						((exception, sqlAction) -> { /*SQL异常处理-SQLExceptionHandler*/ })
				);
	}

	private SQLManager getSQLManager() {
		return sqlManager;
	}

	public DatabaseTable getRankDataTable() {
		return mRankDataTable;
	}
	public DatabaseTable getMPlayerTable() {
		return mPlayerDataTable;
	}
	public DatabaseTable getFPlayerDataTable() {
		return fPlayerDataTable;
	}
	public DatabaseTable getFKitDataTable() {
		return fKitDataTable;
	}

}