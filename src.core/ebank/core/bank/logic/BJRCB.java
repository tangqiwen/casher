/*
 * @Id: BJRCB.java 上午10:52:52 2009-3-17
 * 
 * @author 
 * @version 1.0
 * payment_core PROJECT
 */
package ebank.core.bank.logic;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import com.bjrcb.payment.client.core.MerchantSignVerify;

import ebank.core.bank.BankService;
import ebank.core.common.EventCode;
import ebank.core.common.ServiceException;
import ebank.core.common.util.HandleException;
import ebank.core.domain.BankOrder;
import ebank.core.domain.PayResult;

/**
 * @author 
 * Description: 北京农村商业银行
 * 
 */
public class BJRCB extends BankExt implements BankService{
	private Logger log = Logger.getLogger(this.getClass());
	private String masterid;
	
	/**
	 * @param masterid The masterid to set.
	 */
	public void setMasterid(String masterid) {
		this.masterid = masterid;
	}

	//12位订单号
	public String generateOrderID() throws ServiceException{
		String RandOrderID=sequenceservice.getCode().substring(6,14);	//yyDDDD(6)+8位序列			
		Random rd=new Random();
		String str="";			
		for(int i=0;i<4-this.prefixnum.length();i++) str+=rd.nextInt(10);
		rd=null;		
		return prefixnum+RandOrderID+str;
	}
	
	/* (non-Javadoc)
	 * @see ebank.core.bank.BankService#getPayResult(java.util.HashMap)
	 */
	public PayResult getPayResult(HashMap reqs) throws ServiceException {
		String plain=String.valueOf(reqs.get("Plain"));
		String signature=String.valueOf(reqs.get("Signature"));
		PayResult bean=null;
		try{
			String[] result=plain.split("\\|");			
			log.debug("BJRCB plain:"+plain+" sign:"+signature);			
			Map mp=new HashMap();
			for (int i = 0; i < result.length; i++) {
				String[] ss=result[i].split("\\=");
				if(ss!=null){
					if(ss.length>1)	mp.put(ss[0], ss[1]);
					else mp.put(ss[0], "");
				}
			}
			log.debug(mp);	
			if(MerchantSignVerify.merchantVerifyPayGate_ABA(signature,plain)){//签名验证通过				
				if("00".equals(String.valueOf(mp.get("RespCode")))){
				    bean=new PayResult(String.valueOf(mp.get("TermSsn")),this.bankcode,new BigDecimal(String.valueOf(mp.get("TranAmt"))),1);
				}else{
					bean=new PayResult(String.valueOf(mp.get("TermSsn")),this.bankcode,new BigDecimal(String.valueOf(mp.get("TranAmt"))),-1);				
				}				
				bean.setBankresult(String.valueOf(mp.get("RespCode")));
				bean.setBanktransseq(String.valueOf(mp.get("AcqSsn"))); //银行流水
				bean.setAuthcode(String.valueOf(mp.get("SettDate"))); //清算日期			
			}else{
				throw new ServiceException(EventCode.CRYPT_VALIADATESIGN);				
			}
		}catch (Exception e) {
			HandleException.handle(e);
		}
		return bean;
	}

	/* (non-Javadoc)
	 * @see ebank.core.bank.BankService#sendOrderToBank(ebank.core.domain.BankOrder)
	 */
	public String sendOrderToBank(BankOrder order) throws ServiceException {
		String plain="TranAbbr=IPER|MercDtTm="+order.getPostdate()+"|TermSsn="+order.getRandOrderID()+
		"|OsttDate=|OacqSsn=|MercCode="+this.getCorpid()+"|TermCode=00000000|TranAmt="+order.getAmount()+"|Remark1="+order.getOrderName()+"|Remark2=|"
		+"MercUrl="+this.recurl;
		if(log.isDebugEnabled()) log.info("BJRCB Plain:"+plain);
		String sign=MerchantSignVerify.merchantSignData_ABA(plain);		
		StringBuffer sf=new StringBuffer("");		
		sf.append("<form name=sendOrder method=\"post\" action=\""+this.getDesturl()+"\"/>");
		sf.append("<input type=\"hidden\" name=\"transName\" value=\"IPER\" />");
		sf.append("<input type=\"hidden\" name=\"Plain\" value=\""+plain+"\" />");
		sf.append("<input type=\"hidden\" name=\"Signature\" value=\""+sign+"\" />");		
		return sf.toString();
	}
	

}
