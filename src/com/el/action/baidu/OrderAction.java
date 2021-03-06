package com.el.action.baidu;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Holder;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.el.baidu.api.BaiduConfirm;
import com.el.baidu.api.BaiduConfirmSerializer;
import com.el.baidu.api.BaiduResponse;
import com.el.baidu.api.Cmd;
import com.el.baidu.api.CmdSerializer;
import com.el.connect.DBContextHolder;
import com.el.entity.F0005L;
import com.el.entity.F55wsd02;
import com.el.entity.F55wsd02Id;
import com.el.entity.F55wsd03;
import com.el.entity.F55wsd03Id;
import com.el.entity.Fe14101a;
import com.el.entity.Fe14710a;
import com.el.entity.Fe14710aId;
import com.el.entity.baidu.Discount;
import com.el.entity.baidu.OrderInfo;
import com.el.entity.baidu.OrderStatusInfo;
import com.el.entity.baidu.Product;
import com.el.service.IJDEWebService;
import com.el.service.IOrderService;
import com.el.util.BaiduMapUtil;
import com.el.util.CalculateSignHelp;
import com.el.util.CommonHelper;
import com.el.util.HttpRequestUtil;
import com.el.util.Md5;
import com.el.util.PropertiesUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opensymphony.xwork2.ActionSupport;
@Scope("prototype")
public class OrderAction extends ActionSupport {
	/**
	 * 
	 */
	private static Logger logger = Logger.getLogger(OrderAction.class);

	private static final long serialVersionUID = 1L;
	private OrderInfo body;
	private String cmd;
	private String sign;
	private String source;
	private String ticket;
	private long timestamp;
	private String version;
	private OrderStatusInfo orderStatusInfo;

	public OrderStatusInfo getOrderStatusInfo() {
		return orderStatusInfo;
	}

	public void setOrderStatusInfo(OrderStatusInfo orderStatusInfo) {
		this.orderStatusInfo = orderStatusInfo;
	}

	private IOrderService orderServiceImpl;
	private IJDEWebService jdeWebServiceImpl;

	/**
	 * 所有操作如果成功则返回状态码 0，如果失败状态码为非 0，接口状态码具体释义请参考各接口部分，其他状态码具体释义请参考以下状态码对照表 20100
	 * 请求方法不支持 20101 请求格式不正确 20201 没有权限 20207 账号未上线 10208 参数不合法
	 * 
	 * 10209 配送范围不合法 10222 餐盒费不一致
	 */
	private int errno = 0;
	private String error = "success";

	public IJDEWebService getJdeWebServiceImpl() {
		return jdeWebServiceImpl;
	}

	@Autowired
	public void setJdeWebServiceImpl(IJDEWebService jdeWebServiceImpl) {
		this.jdeWebServiceImpl = jdeWebServiceImpl;
	}

	/**
	 * 返回json
	 */
	private SortedMap<String, Object> data;

	public SortedMap<String, Object> getData() {
		return data;
	}

	public void setData(SortedMap<String, Object> data) {
		this.data = data;
	}

	public IOrderService getOrderServiceImpl() {
		return orderServiceImpl;
	}

	@Autowired
	public void setOrderServiceImpl(IOrderService orderServiceImpl) {
		this.orderServiceImpl = orderServiceImpl;
	}

	public OrderInfo getBody() {
		return body;
	}

	public void setBody(OrderInfo body) {
		this.body = body;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTicket() {
		return ticket;
	}

	public void setTicket(String ticket) {
		this.ticket = ticket;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String orderCreate() throws IOException {
		//先切换一下数据库连接
		DBContextHolder.setDBType(DBContextHolder.DATA_SOURCE_INTERFACE);
		System.out.println("百度外卖:订单推送.......................");
		logger.info("百度外卖:订单推送.......................");
		PropertiesUtil properties = new PropertiesUtil("application.properties");
		String secret = properties.getPropertyByKey("baidusecret");
		HttpServletRequest request = ServletActionContext.getRequest();
		String requestStr = "";
		// getRequestBody(request);
		// 接收order.status.get发送过来的body信息，body信息是一个order_id;
		String strGetBody = "";
		Gson gson = new Gson();
		if (body != null) {
			try {
				JSONObject bodyJsonObj = JSONObject.fromObject(body);
				// System.out.println("百度外卖:订单推送.................");
				System.out.println("百度外卖:订单Body字符串:" + bodyJsonObj.toString());
				logger.info("百度外卖:订单推送.................");
				logger.info("百度外卖:订单Body字符串:" + bodyJsonObj.toString());
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("百度外卖:error:" + e.getMessage(), e);
			}

		}
		if (body == null) {
			Map<String, String[]> reqMap = request.getParameterMap();
			for (String key : reqMap.keySet()) {
				requestStr = key;
				logger.info("百度外卖:订单推送数据:" + requestStr);
				System.out.println("百度外卖:订单推送数据:" + requestStr);
				JSONObject jsonObj = JSONObject.fromObject(key);
				if (jsonObj.getString("cmd") != null) {
					cmd = jsonObj.getString("cmd");
				}

				if (jsonObj.getString("body") != null) {
					if (cmd.equals("order.status.get")) {
						JSONObject jsonGetObj = JSONObject.fromObject(jsonObj
								.getString("body"));
						strGetBody = jsonGetObj.getString("order_id");

					} else if (cmd.equals("order.create")) {
						body = gson.fromJson(jsonObj.getString("body"),
								OrderInfo.class);
					} else if (cmd.equals("order.status.push")) {
						orderStatusInfo = gson.fromJson(
								jsonObj.getString("body"),
								OrderStatusInfo.class);
					}
				}

				if (jsonObj.getString("sign") != null) {
					sign = jsonObj.getString("sign");
				}
				if (jsonObj.getString("source") != null) {
					source = jsonObj.getString("source");
				}
				if (jsonObj.getString("ticket") != null) {
					ticket = jsonObj.getString("ticket");
				}
				if (jsonObj.getString("timestamp") != null) {
					timestamp = gson.fromJson(jsonObj.getString("timestamp"),
							long.class);
				}
				if (jsonObj.getString("version") != null) {
					version = jsonObj.getString("version");
				}
			}

		}
		if (!cmd.equals("order.create") && !cmd.equals("order.status.get")
				&& !cmd.equals("order.status.push")) {
			errno = 20100;
			error = "请求方法不支持";
			data = new TreeMap<String, Object>();
			data.put("cmd", "resp.order.status.push");
			long ltimestamp = CalculateSignHelp.calcTimestamp();
			data.put("timestamp", ltimestamp);
			data.put("version", this.getVersion());
			data.put("ticket", this.getTicket());
			data.put("source", this.getSource());
			data.put("secret", secret);
			// data.put("sign", this.getSign());
			Map<String, Object> body = new TreeMap<String, Object>();
			body.put("errno", errno);
			body.put("error", error);
			// body.put("data", bodydata);
			data.put("body", body);
			// 计算sign
			String sign = CalculateSignHelp.CalculateSign(data);
			data.put("sign", sign);
			data.remove("secret");
			return SUCCESS;

		}

		// get 接口不处理，直接返回
		if (cmd.equals("order.status.get")) {
			data = new TreeMap<String, Object>();
			data.put("cmd", "resp.order.status.get");
			long ltimestamp = CalculateSignHelp.calcTimestamp();
			data.put("timestamp", ltimestamp);
			data.put("version", this.getVersion());
			data.put("ticket", this.getTicket());
			data.put("source", this.getSource());
			data.put("secret", secret);
			// data.put("sign", this.getSign());
			Map<String, Object> body = new TreeMap<String, Object>();
			Map<String, Object> bodydata = new TreeMap<String, Object>();
			bodydata.put("source_order_id", strGetBody);
			bodydata.put("status", 5);
			body.put("errno", errno);
			body.put("error", error);
			body.put("data", bodydata);
			data.put("body", body);
			// 计算sign
			String sign = CalculateSignHelp.CalculateSign(data);
			data.put("sign", sign);
			data.remove("secret");
			return SUCCESS;
			// 订单状态推送 接口,没处理，直接返回
		} else if (cmd.equals("order.status.push")) {
			data = new TreeMap<String, Object>();
			data.put("cmd", "resp.order.status.push");
			long ltimestamp = CalculateSignHelp.calcTimestamp();
			data.put("timestamp", ltimestamp);
			data.put("version", this.getVersion());
			data.put("ticket", this.getTicket());
			data.put("source", this.getSource());
			data.put("secret", secret);
			// data.put("sign", this.getSign());
			Map<String, Object> body = new TreeMap<String, Object>();
			/*
			 * Map<String, Object> bodydata = new TreeMap<String, Object>();
			 * bodydata.put("source_order_id", strGetBody);
			 * bodydata.put("status", 5);
			 */
			body.put("errno", errno);
			body.put("error", error);
			// body.put("data", bodydata);
			data.put("body", body);
			// 计算sign
			String sign = CalculateSignHelp.CalculateSign(data);
			data.put("sign", sign);
			data.remove("secret");
			return SUCCESS;

		}

		// if (body == null || body.getProducts() == null
		// || body.getProducts().size() == 0) {
		// throw new Exception("请求数据，Body没有信息");
		// }

		String[] params = new String[3];
		params[0] = "B2C";
		params[1] = body.getOrder().getOrder_id();
		params[2]="1003";
		List<F55wsd02> lstF55wsd02 = orderServiceImpl.queryF55wsd02(params);
		if (lstF55wsd02 != null && lstF55wsd02.size() > 0) {
			// throw new Exception("重复发送数据");
			logger.info("百度外码:重复发送数据");
			System.out.println("百度外码:重复发送数据");
			logger.info("百度外卖:订单推送数据:" + requestStr);
			System.out.println("百度外卖:订单推送数据:" + requestStr);
			error = error.replace("/", "\\/");
			data = new TreeMap<String, Object>();
			data.put("cmd", "resp.order.create");
			long ltimestamp = CalculateSignHelp.calcTimestamp();
			data.put("timestamp", ltimestamp);
			data.put("version", this.getVersion());
			data.put("ticket", this.getTicket());
			data.put("source", this.getSource());
			data.put("secret", secret);
			// data.put("sign", this.getSign());
			Map<String, Object> bodyMap = new TreeMap<String, Object>();
			Map<String, Object> bodydata = new TreeMap<String, Object>();
			bodydata.put("source_order_id", this.getBody().getOrder()
					.getOrder_id());
			bodyMap.put("errno", errno);
			bodyMap.put("error", error);
			bodyMap.put("data", bodydata);
			data.put("body", bodyMap);
			// 计算sign
			String sign = CalculateSignHelp.CalculateSign(data);
			data.put("sign", sign);
			data.remove("secret");
			return SUCCESS;
		}
		try {
			F55wsd02 f55wsd02 = new F55wsd02();
			F55wsd02Id f55wsd02id = new F55wsd02Id();
			Holder<Integer> ukid = new Holder<Integer>();
			Holder<String> tableName = new Holder<String>("F55WSD02");
			int ihukid = jdeWebServiceImpl.GetNextUniqueID(ukid, tableName);
			if (ihukid == 0) {
				// errno = 10208;
				throw new Exception("获取UKID失败");
			}
			f55wsd02id.setIhukid(new BigDecimal(ihukid));// 双方流水号
			f55wsd02id.setIhe58hedid("B2C");// 交互方向类型 B2C:商城写；JDE:JDE系统写；
			f55wsd02id.setIhedln(new BigDecimal(1000));// 行号，默认1
			f55wsd02.setId(f55wsd02id); // 将来如果1个UKID传多个order_id的时候可以自增此行号，进行订单批量传送
										// .官网写入规则：行号为1，需要乘以1000，再写入;官网取数规则：行号除以1000，再获取；
			// f55wsd02.setIhprukid(ihprukid);// JDE订单唯一编号
			String ihe58eboid = body.getOrder().getOrder_id();
			f55wsd02.setIhe58eboid(ihe58eboid);// B2C订单唯一编号order_id 网单号

			f55wsd02.setIhe58huf06("2");// 接单直接就是确定
			SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm:ss");

			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
			String ihe58hboct = sdf2.format(new Date(body.getOrder()
					.getCreate_time() * 1000L));

			f55wsd02.setIhe58hboct(ihe58hboct);// 订单生成日期 格式为YYYY-MM-DD

			f55wsd02.setIhe58ssot("L1");// 订单种类(58E|OT) JDE提供编码： L1 销售单
			//
			f55wsd02.setIhe58hoca("W");// 下单渠道(58H|OC)W-网络下单；
			// f55wsd02.setIhe58eroid(ihe58eroid);// 记录原始的网单号，退单或者转单时使用

			String ihe58mcno = body.getUser().getPhone();
			f55wsd02.setIhe58mcno(ihe58mcno);// 会员编号

			f55wsd02.setIhe58hbmno(new BigDecimal(0));// B2C订单所有人编号
			// f55wsd02.setIhco(ihco);// 下单公司
			//
			// f55wsd02.setIhan8(ihan8);//
			// 网店号1-对于淘宝、京东等第三方平台来的订单，这个字段不需要填写；2-对于官网开店的(包括1919自营店)，需要将开店的商户编号传进来；商户信息会通过接口，同步到官网
			//
			f55wsd02.setIhe58hus25(body.getShop().getId());// 商户号
			//
			 f55wsd02.setIhmcu("1003");// 表头经营单位
			//
			f55wsd02.setIhdcto("DS");// 订单类型(00|DT)DS 网上销售单；
			//
			f55wsd02.setIhkcoo("00100");// 订单公司
			//
			// f55wsd02.setIhdoco(ihdoco);// 订单号
			//
			// f55wsd02.setIhodoc(ihodoc);// JDE订单号
			// f55wsd02.setIhodct(ihodct);// JDE订单类型
			// f55wsd02.setIhokco(ihokco);// JDE订单公司

			f55wsd02.setIhe58hmn(body.getUser().getName());// 收货人姓名ship_name
			f55wsd02.setIhph1(body.getUser().getPhone());// 收货人电话ship_mobile移动电话
			// f55wsd02.setIhe58mpst(ihe58mpst);// 收货人邮编ship_zip
			// f55wsd02.setIhe58hmail(ihe58hmail);// 收货人邮箱ship_email
			f55wsd02.setIhe58hxxdz(body.getUser().getAddress());// 收货人地址ship_addr详细地址
																// JDE E58MADD

			if (body.getOrder().getDelivery_party() == 2) {
				f55wsd02.setIhe58hshm("R1");// 送货方式（B2C的配送方式）(58H|SM) R1-配送； //
											// R2-自提；R3-百度配送 默认 R1 物流 1 百度 2 自配送
			} else {
				f55wsd02.setIhe58hshm("R3");//
			}

			if (body.getOrder().getNeed_invoice() == 1) {
				f55wsd02.setIhe58hinty("1");// "发票类型(58H|IT)1-普票-正常发票；2-普票-红字发票；3-普票-废票；4-增票；默认1"
			} else {
				f55wsd02.setIhe58hinty("2");// "发票类型(58H|IT)1-普票-正常发票；2-普票-红字发票；3-普票-废票；4-增票；默认1"
			}
			f55wsd02.setIhe58huf07("20");// 开票方法(E1|IM) 10-按明细开； 20-按表头开； 默认20
			f55wsd02.setIhe58hintl(body.getOrder().getInvoice_title());// 发票抬头tax_company
			f55wsd02.setIhe58hos("516");// 订单总状态 516 未处理
			f55wsd02.setIhe58hus14("00001");// 开票内容，同步到F58H0401.E58HUS14 E1|IL
											// 00001-餐费；如果开票方式为20，则此字段必须有值，默认00001
			if (body.getOrder().getRemark() != null) {
				String remark=body.getOrder().getRemark().trim();
				
				if(remark.length()>40){
					remark=remark.substring(0, 39);
				}
				f55wsd02.setIhe58hus24(remark);// 买家留言 备注
				
				String memo=body.getOrder().getRemark().trim();
				if(memo.length()>100){
					memo=memo.substring(0,99);
				}
				f55wsd02.setIhe58hus26(memo);//订单备注
				
			}

			f55wsd02.setIhev01("N");// 读取标示 默认传N N代表未读，Y表示已读，B2C只读N

			// SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm:ss");
			String ihe58hsjut = sdf1.format(new Date(body.getOrder()
					.getCreate_time() * 1000L));
			// System.out.println(ihe58hsjut);
			f55wsd02.setIhe58hsjut(ihe58hsjut);// 订单生成时间 格式：hh:mm:ss 订单创建时间
												// order.create_time
			SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String ihe58hsbut = sdf3.format(new Date(body.getOrder()
					.getCreate_time() * 1000L));
			f55wsd02.setIhe58hsbut(ihe58hsbut);// "B2C更新时间		数据格式：yyyy-MM-dd HH:mm:SS"
			f55wsd02.setIhcomitlvl(new BigDecimal("2"));// 订单配送优先级(01|PT)2 普通送；
			String ihe58huf02 = body.getOrder().getPay_type();
			if (ihe58huf02.equals("1")) {
				f55wsd02.setIhe58gpayf("1");// 付款标识 未付款为1；已付款为3；
			} else {
				f55wsd02.setIhe58gpayf("3");// 付款标识 未付款为1；已付款为3；
			}

			f55wsd02.setIhe58huf02(ihe58huf02);// "支付方式   对应JDE E58HSSPF1， 货到付款2， 在线支付 "
			f55wsd02.setIhe58hpaym("01");// "付款方式 对应JDE 付款方式表FE4PT001 JDE处理 01
											// 现金 02 POS刷卡 04 微信支付 07 支付宝

			f55wsd02.setIhe58tsua(new BigDecimal(body.getOrder().getTotal_fee()));// 应收总金额
			int discountfee = body.getOrder().getDiscount_fee();
			f55wsd02.setIhe58oda(new BigDecimal(discountfee));// 整单折让金额

			f55wsd02.setIhe58hus23("1003");// " 1001 官网    1002 微信 E1/WT	F58H0401.MCU 1003 百度 1010美团 1006饿了么"
			int ihe58jua01 = body.getProducts().size() * 100;

			f55wsd02.setIhdl01(ihe58hsjut);// 要求送货时间从，格式为HH:mm:ss

			String Longitude = "";
			String Latitude = "";
			JSONObject mapJsonObj = null;
			if (body.getUser().getCoord() != null) {
				Longitude = body.getUser().getCoord().getLongitude();// 经度
				Latitude = body.getUser().getCoord().getLatitude();// 纬度
				mapJsonObj = BaiduMapUtil.calcGeocoding(Latitude, Longitude,
						"bd09ll");
			} else if (body.getUser().getCoord_amap() != null) {
				Longitude = body.getUser().getCoord_amap().getLongitude();// 经度
				Latitude = body.getUser().getCoord_amap().getLatitude();// 纬度
			}
			f55wsd02.setIhdl02(Longitude);// 经度 接百度
			f55wsd02.setIhdl03(Latitude);// 纬度

			f55wsd02.setIhe58brd2("CN");// 国家,默认CN(中国)
			if (mapJsonObj != null) {
				Object[] mapParams = new Object[2];
				mapParams[0] = "PR";// CT// PR// PT
				mapParams[1] = mapJsonObj.getString("province");
				List<F0005L> lstF0005ls = orderServiceImpl
						.queryLstF0005L(mapParams);
				if (lstF0005ls != null && lstF0005ls.size() > 0) {
					f55wsd02.setIhe58hproc(lstF0005ls.get(0).getId().getDrky());// 省（58|PR），省的编码，必填
				}

				mapParams[0] = "CT";// CT
				mapParams[1] = mapJsonObj.getString("city");
				lstF0005ls = orderServiceImpl.queryLstF0005L(mapParams);
				if (lstF0005ls != null && lstF0005ls.size() > 0) {
					f55wsd02.setIhe58hcity(lstF0005ls.get(0).getId().getDrky());// 市

				}

				mapParams[0] = "PT";
				mapParams[1] = mapJsonObj.getString("district");
				lstF0005ls = orderServiceImpl.queryLstF0005L(mapParams);
				if (lstF0005ls != null && lstF0005ls.size() > 0) {
					f55wsd02.setIhe58hpref(lstF0005ls.get(0).getId().getDrky());// 区
				}
			} else {
				f55wsd02.setIhe58hproc("2");// 省（58|PR），省的编码，必填 商户所在省名称 province
				f55wsd02.setIhe58hcity("52");// 市
				f55wsd02.setIhe58hpref("500");// 区（县）
				f55wsd02.setIhe58hbzne(null);// 区域编码
			}

			f55wsd02.setIhe58jua01(new BigDecimal(ihe58jua01));// 订单明细行的行数
																// 传入时，需要将计算出的行数*100后，写入该字段

			int send_immediately = body.getOrder().getSend_immediately();
			String sendtime = "";// 要求送货时间
			Date d = new Date();
			String ihe58hdrqd = sdf2.format(d);// 要求送货日期，格式为YYYY-MM-DD
			if (send_immediately == 1) {

				sendtime = sdf1.format(new Date(body.getOrder()
						.getCreate_time() * 1000L + 3600 * 1000L));// *******默认是加1个小时
			} else {
				try {
					long lsendTime = Long.parseLong(body.getOrder()
							.getSend_time());
					sendtime = sdf1.format(new Date(lsendTime * 1000L));

					ihe58hdrqd = sdf2.format(new Date(lsendTime * 1000L));

					String sendstarttime = sdf1.format(new Date(
							lsendTime * 1000L - 3600 * 1000L));
					Calendar calSenddate = Calendar.getInstance();
					Calendar calCreatedate = Calendar.getInstance();

					Date sendStartdate = new Date(
							lsendTime * 1000L - 3600 * 1000L);
					Date createDate = new Date(
							body.getOrder().getCreate_time() * 1000L);

					calSenddate.setTime(sendStartdate);
					calCreatedate.setTime(createDate);
					// 如果 是减去1个小时，变成当天，要求送货日期就要改成当天
					if (calSenddate.get(Calendar.DAY_OF_MONTH) == calCreatedate
							.get(Calendar.DAY_OF_MONTH)) {
						ihe58hdrqd = sdf2.format(calCreatedate.getTime());
					} else {
						ihe58hdrqd = sdf2.format(calSenddate.getTime());
					}

					f55wsd02.setIhdl01(sendstarttime);// 要求送货时间从，格式为HH:mm:ss
				} catch (Exception e) {
					sendtime = ihe58hsjut;
					ihe58hdrqd = sdf2.format(d);// 要求送货日期，格式为YYYY-MM-DD
					logger.error("要求送货时间，转换失败，发送的send_time为"
							+ body.getOrder().getSend_time(), e);
				}

			}
			f55wsd02.setIhe58hdrqd(ihe58hdrqd);// 要求送货日期，格式为YYYY-MM-DD
			f55wsd02.setIhtxt2(sendtime);// 要求送货时间到，格式为HH:mm:ss
			int i = 0;

			List<F55wsd03> lstF55wsd03 = new ArrayList<F55wsd03>();
			for (Product p : body.getProducts()) {

				// f55wsd03.setIdco(idco);//下单公司
				Object[] params1 = new Object[2];
				params1[0] = "01";
				// params1[1] = body.getShop().getId();
				String[] productParms = p.getProduct_name().split("_");
				String srtProd = "";
				for (int pi = 0; pi < productParms.length; pi++) {

					srtProd = srtProd + productParms[pi];

				}
				params1[1] = srtProd;
				List<Fe14101a> lstFe14101a = orderServiceImpl
						.queryFe14101a(params1);
				if (lstFe14101a == null || lstFe14101a.size() == 0) {
					throw new Exception("百度外卖:创建订单失败，根据商品名称:"
							+ p.getProduct_name() + ";商户ID:"
							+ body.getShop().getId()
							+ ";找不到系统中对应的商品数据(Fe14101a)");
				}

				// 单品汇总价值
				double sumTotal = CommonHelper.calcSumTotal(lstFe14101a);
				double reduceTotal = 0;
				// 组合菜品总价
				double combineTotal = p.getProduct_price();
				for (int j = 0; j < lstFe14101a.size(); j++) {
					F55wsd03 f55wsd03 = new F55wsd03();
					F55wsd03Id f55wsd03id = new F55wsd03Id();
					i = i + 1;
					f55wsd03id.setIdukid(new BigDecimal(ihukid));// 双方流水号
																	// 自然数流水如：1,2，3。。。

					f55wsd03id.setIde58hedid("B2C");// "交互方向类型	 B2C:商城写；	 	JDE:JDE系统写；"

					f55wsd03id.setIdedln(new BigDecimal(1000));// "行号，此行号与F55WSD02.EDLN保持一致 官网写入规则：行号为1，需要乘以1000，再写入；官网取数规则：行号除以1000，再获取；"

					f55wsd03id.setIdlnic(new BigDecimal(i * 1000));// "行号，同一个UKID从1开始，每行增加1 官网写入规则：行号为1，需要乘以1000，再写入；官网取数规则：行号除以1000，再获取；"
					f55wsd03.setId(f55wsd03id);
					// f55wsd03.setIde58hlnid(ide58hlnid);// 零售单行号

					f55wsd03.setIde58eboid(body.getOrder().getOrder_id());
					;// 网单编号Order_id 网单号
					f55wsd03.setIde58hboct(f55wsd02.getIhe58hboct());// "订单生成日期			格式为YYYY-MM-DD"

					f55wsd03.setIde58ssot("L1");// "订单种类(58E|OT)	JDE提供编码：		L1 销售单"

					f55wsd03.setIde58hoca("W");// "下单渠道(58H|OC)				W-网络下单；"
					// 单品原价
					double singlePrice = lstFe14101a.get(j).getPtuprc()
							.intValue() / 100;
					double price = 0;
					if (j == (lstFe14101a.size() - 1)) {
						price = (combineTotal - reduceTotal)
								/ lstFe14101a.get(j).getPtqnty().intValue();
					} else {
						price = singlePrice - (sumTotal - combineTotal)
								* singlePrice / combineTotal;
					}

					reduceTotal = reduceTotal + price
							* lstFe14101a.get(j).getPtqnty().intValue();

					String idlitm = lstFe14101a.get(j).getPtlitm();
					// idlitm = "MX";
					f55wsd03.setIdlitm(idlitm);// B2C商品的唯一标示ID，bn JDE商品编号
					// 网站商品编号
					double ptQnty = lstFe14101a.get(j).getPtqnty().intValue();
					
					f55wsd03.setIdtxtlfu3((long)(ptQnty));//插入转换系数
					
					double iduorg = ptQnty * p.getProduct_amount() * 1000L;
					f55wsd03.setIduorg(new BigDecimal(iduorg));// "nums	JDE是整数的，包含3位小数，B2C转化成乘以1000；官网写入规则：行号为1，需要乘以1000，再写入；官网取数规则：行号除以1000，再获取；"
					// int price = p.getProduct_price();
					f55wsd03.setIde58pael(new BigDecimal(price));// "price   成交价		JDE是整数的，包含2位小数，B2C转化到分，乘以100；		官网写入规则：行号为1，需要乘以100，再写入；				官网取数规则：行号除以100，再获取；"
					double productFee = iduorg * price / 1000;
					f55wsd03.setIde58aexp(new BigDecimal(productFee));// "amount   成交金额	JDE是整数的，包含2位小数，B2C转化到分，乘以100；官网写入规则：行号为1，需要乘以100，再写入；官网取数规则：行号除以100，再获取；"

					f55wsd03.setIde58gpayf(f55wsd02.getIhe58gpayf());// "付款标识 未付款为1；已付款为3；"

					f55wsd03.setIde58hos("516");// "商品行状态			516"

					f55wsd03.setIdev02("N");// "读取标示N代表未读，Y表示已读	B2C只读N"

					f55wsd03.setIde58hsctm(ihe58hsbut);// "创建时间yyyy-MM-dd hh:mm:ss	INSERT记录的时间"

					f55wsd03.setIde58hsjut(ihe58hsjut);// "订单生成时间格式：hh:mm:ss"

					f55wsd03.setIde58hsbut(ihe58hsbut);// B2C更新时间，数据格式：yyyy-MM-dd
														// hh:mm:ss

					f55wsd03.setIde58hus21(p.getProduct_name());// 商品打印名称

					f55wsd03.setIdan03(new BigDecimal(price));// 会员价
					f55wsd03.setIdan04(new BigDecimal(productFee));// 会员金额
					lstF55wsd03.add(f55wsd03);
				}

			}
			if (body.getOrder().getSend_fee() > 0) {
				i = i + 1;
				F55wsd03 f55wsd03 = genWaiSongFei(f55wsd02, i, body.getOrder()
						.getSend_fee());
				lstF55wsd03.add(f55wsd03);
			}

			List<Fe14710a> lstfe14710a = new ArrayList<Fe14710a>();

			lstfe14710a = genFe14710a(f55wsd02);

			orderServiceImpl.saveOrder(f55wsd02, lstF55wsd03, lstfe14710a);

			orderServiceImpl.callWebservice(f55wsd02, lstF55wsd03);

			try {
				BaiduConfirm baiduConfirm = new BaiduConfirm();
				baiduConfirm.setOrder_id(body.getOrder().getOrder_id());
				BaiduResponse baiduConfirmResponse = dochangeBaiduConfirm(baiduConfirm);
				if (baiduConfirmResponse.getErrno() != 0) {
					String errmessage = "百度外卖:确认失败,失败信息:orderid="
							+ body.getOrder().getOrder_id() + ",error:"
							+ baiduConfirmResponse.getError();
					System.out.println(errmessage);
					logger.error(errmessage);
				}
			} catch (Exception e) {
				String errmessage = "百度外卖:确认失败,失败信息:orderid="
						+ body.getOrder().getOrder_id() + ",error:"
						+ e.getMessage();
				System.out.println(errmessage);
				logger.error(errmessage);

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			/*
			 * 注释，失败不返回失败，直接返回成功，不然订单会被直接取消，这样，可以让商家后台进行接单
			 */
			// if (errno == 0) {
			// errno = 20101;
			// }
			// error = e.getMessage();

			e.printStackTrace();

			logger.error(e.getMessage(), e);
		} finally {
			error = error.replace("/", "\\/");
			data = new TreeMap<String, Object>();
			data.put("cmd", "resp.order.create");
			long ltimestamp = CalculateSignHelp.calcTimestamp();
			data.put("timestamp", ltimestamp);
			data.put("version", this.getVersion());
			data.put("ticket", this.getTicket());
			data.put("source", this.getSource());
			data.put("secret", secret);
			// data.put("sign", this.getSign());
			Map<String, Object> bodyMap = new TreeMap<String, Object>();
			Map<String, Object> bodydata = new TreeMap<String, Object>();
			bodydata.put("source_order_id", this.getBody().getOrder()
					.getOrder_id());
			bodyMap.put("errno", errno);
			bodyMap.put("error", error);
			bodyMap.put("data", bodydata);
			data.put("body", bodyMap);
			// 计算sign
			String sign = CalculateSignHelp.CalculateSign(data);
			data.put("sign", sign);
			data.remove("secret");

		}
		return SUCCESS;
	}

	private List<Fe14710a> genFe14710a(F55wsd02 f55wsd02) {
		List<Fe14710a> lstfe14710a = new ArrayList<Fe14710a>();
		Fe14710a fe14710a = new Fe14710a();
		Fe14710aId fe14710aid = new Fe14710aId();
		fe14710aid.setLsukid(f55wsd02.getId().getIhukid());
		// ihe58huf02 支付类型，1 下线 2 在线
		// 付款方式UDC：E1/PT 01 现金 ; 09 百度外卖在线支付
		if (f55wsd02.getIhe58huf02().equals("1")) {
			fe14710aid.setLse58pt("01");
		} else {
			fe14710aid.setLse58pt("09");
		}
		fe14710a.setId(fe14710aid);
		fe14710a.setLsflg("N");
		// 应收金额
		fe14710a.setLsaap(new BigDecimal(body.getOrder().getShop_fee()));
		// 实收金额
		 fe14710a.setLsag(fe14710a.getLsaap());
		lstfe14710a.add(fe14710a);
		// 优惠
		if (body.getOrder().getDiscount_fee() > 0) {
			if (body.getDiscount().size() > 0) {
				double baidurate = 0;// 百度承担
				double shoprate = 0;// 商户承担
				for (Discount discount : body.getDiscount()) {
					/*
					 * jian 立减优惠 立减N元；满M元减N元 mian 免配送费 下单免配送费；满 N 元免配送费 xin
					 * 新用户立减 新用户立减 N 元 zeng 下单满赠 订单满 M 元，赠 A x 份，如：加多宝 2 份
					 * payenjoy 在线支付营销 订单满 M 元，使用在线支付立减 N 元 preorder 预订优惠
					 * 在指定时间内，订单满 M 元，立减 N 元 fan 返券优惠 订单满 M 元，返 A 代金卷 x 张 coupon
					 * 代金券优惠 代金券信息
					 */
					System.out.println("百度:优惠信息" + discount.getType() + ";"
							+ discount.getDesc());
					logger.info("百度:优惠信息" + discount.getType() + ";"
							+ discount.getDesc());
					if (discount.getBaidu_rate() == null
							|| discount.getBaidu_rate().isEmpty()) {
						discount.setBaidu_rate("0");
					}
					if (discount.getShop_rate() == null
							|| discount.getShop_rate().isEmpty()) {
						discount.setShop_rate("0");
					}
					baidurate = baidurate
							+ Double.parseDouble(discount.getBaidu_rate());
					shoprate = shoprate
							+ Double.parseDouble(discount.getShop_rate());

				}

				if (baidurate > 0) {
					Fe14710a fe14710aDiscount = new Fe14710a();
					Fe14710aId fe14710aDiscountid = new Fe14710aId();
					fe14710aDiscountid.setLsukid(f55wsd02.getId().getIhukid());

					// 付款方式UDC：E1/PT 01 现金 ; 09 百度外卖在线支付 22百度挂账，23美团挂账 ；25饿了么挂账
					// 22是百度挂账，就是百度承担的优惠
					fe14710aDiscountid.setLse58pt("22");

					fe14710aDiscount.setId(fe14710aDiscountid);
					fe14710aDiscount.setLsflg("N");

					// 应收金额
					fe14710aDiscount.setLsaap(new BigDecimal(baidurate));
					fe14710aDiscount.setLsag(fe14710aDiscount.getLsaap());
					lstfe14710a.add(fe14710aDiscount);
				}

				if (shoprate > 0) {
					Fe14710a fe14710aDiscount = new Fe14710a();
					Fe14710aId fe14710aDiscountid = new Fe14710aId();
					fe14710aDiscountid.setLsukid(f55wsd02.getId().getIhukid());

					// 付款方式UDC：E1/PT 01 现金 ; 09 百度外卖在线支付 22百度挂账，23美团挂账
					// ；25饿了么挂账;29优惠券
					// 22是百度挂账，就是百度承担的优惠; 29就是优惠券，商户承担
					fe14710aDiscountid.setLse58pt("29");

					fe14710aDiscount.setId(fe14710aDiscountid);
					fe14710aDiscount.setLsflg("N");

					// 应收金额
					fe14710aDiscount.setLsaap(new BigDecimal(shoprate));
					fe14710aDiscount.setLsag(fe14710aDiscount.getLsaap());
					lstfe14710a.add(fe14710aDiscount);
				}
			}

		}
		return lstfe14710a;
	}

	private F55wsd03 genWaiSongFei(F55wsd02 f55wsd02, int rownum, int price) {
		F55wsd03 f55wsd03 = new F55wsd03();
		F55wsd03Id f55wsd03id = new F55wsd03Id();

		f55wsd03id.setIdukid(f55wsd02.getId().getIhukid());// 双方流水号
															// 自然数流水如：1,2，3。。。

		f55wsd03id.setIde58hedid("B2C");// "交互方向类型	 B2C:商城写；	 	JDE:JDE系统写；"

		f55wsd03id.setIdedln(new BigDecimal(1000));// "行号，此行号与F55WSD02.EDLN保持一致 官网写入规则：行号为1，需要乘以1000，再写入；官网取数规则：行号除以1000，再获取；"

		f55wsd03id.setIdlnic(new BigDecimal(rownum * 1000));// "行号，同一个UKID从1开始，每行增加1 官网写入规则：行号为1，需要乘以1000，再写入；官网取数规则：行号除以1000，再获取；"
		f55wsd03.setId(f55wsd03id);
		// f55wsd03.setIde58hlnid(ide58hlnid);// 零售单行号

		f55wsd03.setIde58eboid(body.getOrder().getOrder_id());
		;// 网单编号Order_id 网单号
		f55wsd03.setIde58hboct(f55wsd02.getIhe58hboct());// "订单生成日期			格式为YYYY-MM-DD"

		f55wsd03.setIde58ssot("L1");// "订单种类(58E|OT)	JDE提供编码：		L1 销售单"

		f55wsd03.setIde58hoca("W");// "下单渠道(58H|OC)				W-网络下单；"

		// f55wsd03.setIdco(idco);//下单公司

		String idlitm = "WSF";// 外送费
		f55wsd03.setIdlitm(idlitm);// B2C商品的唯一标示ID，bn JDE商品编号

		/*
		 * String ide58hbitm = p.getProduct_id(); ide58hbitm = "121"; if
		 * (ide58hbitm == null || ide58hbitm.equals("")) { throw new
		 * Exception("创建订单失败，没有商品ID，商品名称:" + p.getProduct_name()); }
		 */

		// f55wsd03.setIde58hbitm(new BigDecimal(idlitm));// B2C商品唯一代码
		// 网站商品编号

		Long iduorg = 1 * 1000L;

		f55wsd03.setIduorg(new BigDecimal(iduorg));// "nums	JDE是整数的，包含3位小数，B2C转化成乘以1000；官网写入规则：行号为1，需要乘以1000，再写入；官网取数规则：行号除以1000，再获取；"

		f55wsd03.setIde58pael(new BigDecimal(price));// "price   成交价		JDE是整数的，包含2位小数，B2C转化到分，乘以100；		官网写入规则：行号为1，需要乘以100，再写入；				官网取数规则：行号除以100，再获取；"

		f55wsd03.setIde58aexp(new BigDecimal(price));// "amount   成交金额	JDE是整数的，包含2位小数，B2C转化到分，乘以100；官网写入规则：行号为1，需要乘以100，再写入；官网取数规则：行号除以100，再获取；"

		f55wsd03.setIde58gpayf(f55wsd02.getIhe58gpayf());// "付款标识 未付款为1；已付款为3；"

		f55wsd03.setIde58hos("516");// "商品行状态			516"

		f55wsd03.setIdev02("N");// "读取标示N代表未读，Y表示已读	B2C只读N"

		f55wsd03.setIde58hsctm(f55wsd02.getIhe58hsbut());// "创建时间yyyy-MM-dd hh:mm:ss	INSERT记录的时间"

		f55wsd03.setIde58hsjut(f55wsd02.getIhe58hsjut());// "订单生成时间格式：hh:mm:ss"

		f55wsd03.setIde58hsbut(f55wsd02.getIhe58hsbut());// B2C更新时间，数据格式：yyyy-MM-dd
		// hh:mm:ss

		f55wsd03.setIde58hus21("外送费");// 商品打印名称

		f55wsd03.setIdan03(new BigDecimal(price));// 会员价
		f55wsd03.setIdan04(new BigDecimal(price));// 会员金额
		return f55wsd03;
	}

	public BaiduResponse dochangeBaiduConfirm(BaiduConfirm baiduConfirm) {
		PropertiesUtil properties = new PropertiesUtil("application.properties");
		// 准备body数据
		String secret = properties.getPropertyByKey("baidusecret");
		String strSource = properties.getPropertyByKey("baidusource");
		String url = properties.getPropertyByKey("baiduurl");
		// 准备CMD签名计算数据
		Cmd cmd = new Cmd();
		cmd.setCmd("order.confirm");
		cmd.setSource(Integer.parseInt(strSource));
		cmd.setSecret(secret);
		cmd.setTicket(UUID.randomUUID().toString().toUpperCase());
		cmd.setTimestamp((int) (System.currentTimeMillis() / 1000));
		cmd.setVersion(2);
		cmd.setBody(baiduConfirm);
		cmd.setSign(null);

		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Cmd.class, new CmdSerializer())
				.registerTypeAdapter(BaiduConfirm.class,
						new BaiduConfirmSerializer()).disableHtmlEscaping()
				.create();
		String signJson = gson.toJson(cmd);
		// 对所有/进行转义
		signJson = signJson.replace("/", "\\/");
		// 中文字符转为unicode
		signJson = Md5.chinaToUnicode(signJson);
		System.out.println(signJson);
		String sign = Md5.getMD5(signJson);

		// 准备生成请求数据，此处注意secret不参与传递，故设置为null
		cmd.setSign(sign);
		cmd.setSecret(null);
		String requestJson = gson.toJson(cmd);
		// 对所有/进行转义
		requestJson = requestJson.replace("/", "\\/");
		// 中文字符转为unicode
		requestJson = Md5.chinaToUnicode(requestJson);

		System.out.println(requestJson);

		JSONObject jsonObj = HttpRequestUtil.httpBaiduPost(url, requestJson);
		JSONObject bodyJsonObj = jsonObj.getJSONObject("body");
		int errno = bodyJsonObj.getInt("errno");
		String error = bodyJsonObj.getString("error");

		BaiduResponse baiduResponse = new BaiduResponse(errno, error);
		return baiduResponse;
	}

}
