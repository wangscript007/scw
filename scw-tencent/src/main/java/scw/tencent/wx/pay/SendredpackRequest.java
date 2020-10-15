package scw.tencent.wx.pay;

import java.io.Serializable;

import scw.lang.Nullable;

/**
 * {@link https://pay.weixin.qq.com/wiki/doc/api/tools/cash_coupon.php?chapter=13_4&index=3}
 * @author shuchaowen
 *
 */
public class SendredpackRequest implements Serializable{
	private static final long serialVersionUID = 1L;
	/**
	 * 商户订单号（每个订单号必须唯一。取值范围：0~9，a~z，A~Z）接口根据商户订单号支持重入，如出现超时可再调用。
	 */
	private String mch_billno;
	/**
	 * 微信分配的公众账号ID（企业号corpid即为此appId）。在微信开放平台（open.weixin.qq.com）申请的移动应用appid无法使用该接口。
	 */
	private String wxappid;
	/**
	 * 红包发送者名称 注意：敏感词会被转义成字符*
	 */
	private String send_name;
	/**
	 * openid为用户在wxappid下的唯一标识
	 */
	private String re_openid;
	/**
	 * 付款金额，单位分
	 */
	private int total_amount;
	/**
	 * 红包发放总人数
	 */
	private int total_num;
	/**
	 * 红包祝福语 注意：敏感词会被转义成字符*
	 */
	private String wishing;
	/**
	 * 调用接口的机器Ip地址
	 */
	private String client_ip;
	/**
	 * 活动名称 注意：敏感词会被转义成字符*
	 */
	private String act_name;
	/**
	 * 备注信息
	 */
	private String remark;
	/**
	 * 发放红包使用场景，红包金额大于200或者小于1元时必传
	 * PRODUCT_1:商品促销<br/>
	 * PRODUCT_2:抽奖<br/>
	 * PRODUCT_3:虚拟物品兑奖 <br/>
	 * PRODUCT_4:企业内部福利<br/>
	 * PRODUCT_5:渠道分润<br/>
	 * PRODUCT_6:保险回馈<br/>
	 * PRODUCT_7:彩票派奖<br/>
	 * PRODUCT_8:税务刮奖
	 */
	@Nullable
	private String scene_id;
	/**
	 * 活动信息<br/>
	 * 
	 * posttime:用户操作的时间戳<br/>
	 * mobile:业务系统账号的手机号，国家代码-手机号。不需要+号<br/>
	 * deviceid :mac 地址或者设备唯一标识 <br/>
	 * clientversion :用户操作的客户端版本<br/>把值为非空的信息用key=value进行拼接，再进行urlencode<br/>
	 * urlencode(posttime=xx& mobile =xx&deviceid=xx)
	 */
	@Nullable
	private String risk_info;
	
	/**
	 * @return {@link #mch_billno}
	 */
	public String getMch_billno() {
		return mch_billno;
	}
	public void setMch_billno(String mch_billno) {
		this.mch_billno = mch_billno;
	}
	public String getWxappid() {
		return wxappid;
	}
	public void setWxappid(String wxappid) {
		this.wxappid = wxappid;
	}
	public String getSend_name() {
		return send_name;
	}
	public void setSend_name(String send_name) {
		this.send_name = send_name;
	}
	public String getRe_openid() {
		return re_openid;
	}
	public void setRe_openid(String re_openid) {
		this.re_openid = re_openid;
	}
	public int getTotal_amount() {
		return total_amount;
	}
	public void setTotal_amount(int total_amount) {
		this.total_amount = total_amount;
	}
	public int getTotal_num() {
		return total_num;
	}
	public void setTotal_num(int total_num) {
		this.total_num = total_num;
	}
	public String getWishing() {
		return wishing;
	}
	public void setWishing(String wishing) {
		this.wishing = wishing;
	}
	public String getClient_ip() {
		return client_ip;
	}
	public void setClient_ip(String client_ip) {
		this.client_ip = client_ip;
	}
	public String getAct_name() {
		return act_name;
	}
	public void setAct_name(String act_name) {
		this.act_name = act_name;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public String getScene_id() {
		return scene_id;
	}
	public void setScene_id(String scene_id) {
		this.scene_id = scene_id;
	}
	public String getRisk_info() {
		return risk_info;
	}
	public void setRisk_info(String risk_info) {
		this.risk_info = risk_info;
	}
}
