package com.entie.fir.mainpack.vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Window;
import org.zkoss.zul.event.PagingEvent;

import com.entie.fir.base.vm.BaseVm;
import com.entie.fir.common.bean.PagingBean;
import com.entie.fir.common.bean.SessionInfoBean;
import com.entie.fir.common.bean.UserProfileBean;
import com.entie.fir.common.constant.FirEvent;
import com.entie.fir.common.constant.ZulPage;
import com.entie.fir.mainpack.constant.CaseFlow;
import com.entie.fir.mainpack.constant.CaseFunction;
import com.entie.fir.mainpack.constant.Role;
import com.entie.fir.mainpack.param.CaseParam;
import com.entie.fir.mainpack.service.CaseService;
import com.entie.fir.mainpack.vo.CaseVo;
import com.entie.fir.mainpack.vo.FirFunctionStatusVo;
import com.entie.fir.util.LogUtils;
import com.entie.fir.zk.Msg;
import com.entie.fir.zk.SessionUtil;

@AfterCompose(superclass = true)
public class CaseReviewTaskMainVm extends BaseVm{
	
	@WireVariable("CaseService")
	private CaseService caseService;
	
	private CaseParam caseParam;
	
	private PagingBean<CaseVo> pagingBeanInsTask = new PagingBean<CaseVo>();
	private PagingBean<CaseVo> pagingBeanInsTempTask = new PagingBean<CaseVo>();
	private PagingBean<CaseVo> pagingBeanInsAbortTask = new PagingBean<CaseVo>();
	private PagingBean<CaseVo> pagingBeanExceptionTask = new PagingBean<CaseVo>();
	
	private boolean insTaskVisible = true;
	private boolean insTempTaskVisible = true;
	private boolean insAbortTaskVisible = true;
	private boolean insExceptionTaskVisible = true;
	
	private UserProfileBean userProfile = SessionUtil.getUserProfile();
	
	private Map<String,Object> caseFlowMap = new HashMap<String,Object>();
	
	@Init(superclass = true)
	public void init() {
		try {
			
			Map<?, ?> arg = Executions.getCurrent().getArg();
			caseFlowMap = (Map<String,Object>)arg.get("caseFlowMap");

			caseParam = new CaseParam(CaseFunction.FUN_REVIEW_TASK);
			caseParam = (CaseParam)arg.get("taskDetailCaseParam");
			this.initQueryReviewDetail();
			
		} catch (Exception e) {
			e.printStackTrace();
			String methodName = "init()";
			String logMsg = LogUtils.createLogMsg(getClass(), methodName);
			logger.error(logMsg, e);
			Msg.showError(e);
		}
	}
	
	private void initQueryReviewDetail() throws Exception{
		
		String role = userProfile.getRoleNoList().get(0);
		
		if(Role.INSTLeader.equals(role)){
			
			//依照前面搜尋到的明細，判斷是否要搜尋特定案件流程保單
			if(CaseFlow.FLOW_2.equals(MapUtils.getString(caseFlowMap, CaseFlow.FLOW_2, ""))){
				this.doQuery(0, pagingBeanInsTask, "pagingBeanInsTask", CaseFlow.FLOW_2);
			}
			if(CaseFlow.FLOW_4.equals(MapUtils.getString(caseFlowMap, CaseFlow.FLOW_4, ""))){
				this.doQuery(0, pagingBeanInsTempTask, "pagingBeanInsTempTask", CaseFlow.FLOW_4);
			}
			if(CaseFlow.FLOW_8.equals(MapUtils.getString(caseFlowMap, CaseFlow.FLOW_8, ""))){
				this.doQuery(0, pagingBeanInsAbortTask, "pagingBeanInsAbortTask", CaseFlow.FLOW_8);
			}
			if(CaseFlow.FLOW_9.equals(MapUtils.getString(caseFlowMap, CaseFlow.FLOW_9, ""))){
				this.doQuery(0, pagingBeanExceptionTask, "pagingBeanExceptionTask", CaseFlow.FLOW_9);
			}
			
			insTaskVisible = pagingBeanInsTask.getList() == null ? false : true;
			insTempTaskVisible = pagingBeanInsTempTask.getList() == null ? false : true;
			insAbortTaskVisible = pagingBeanInsAbortTask.getList() == null ? false : true;
			insExceptionTaskVisible = pagingBeanExceptionTask.getList() == null ? false : true;
			
			pagingBeanInsTask.setSelectedItems(null);
			pagingBeanInsTempTask.setSelectedItems(null);
			pagingBeanInsAbortTask.setSelectedItems(null);
			pagingBeanExceptionTask.setSelectedItems(null);
		
		}else if(Role.INSGLeader.equals(role)){
			
			if(CaseFlow.FLOW_9.equals(MapUtils.getString(caseFlowMap, CaseFlow.FLOW_9, ""))){
				this.doQuery(0, pagingBeanExceptionTask, "pagingBeanExceptionTask", CaseFlow.FLOW_9);
			}
			
			insTaskVisible = false;
			insTempTaskVisible = false;
			insAbortTaskVisible = false;
			insExceptionTaskVisible = pagingBeanExceptionTask.getList() == null ? false : true;
			
			pagingBeanExceptionTask.setSelectedItems(null);
			
		}
		
		BindUtils.postNotifyChange(null, null, this, "*");
		
		//如果四種案件流程都已經覆核完畢，則回到搜尋覆核清單頁面
		if(!insTaskVisible && !insTempTaskVisible && !insAbortTaskVisible && !insExceptionTaskVisible){
			Events.postEvent(view, new Event(FirEvent.ON_CLOSE_WINDOW));
			view.detach();
		}
	}
	
	@Command("onPaging")
	public void onPaging(@BindingParam("pagingEvent") PagingEvent pagingEvent, @BindingParam("pageBean") PagingBean<CaseVo> pageBean, @BindingParam("notifyChangePageBean") String pageBeanName, @BindingParam("flow") String flow) {
		try {
			
			this.doQuery(pagingEvent.getActivePage(), pageBean, pageBeanName, flow);
			
		} catch (Exception e) {
			e.printStackTrace();
			String logMsg = LogUtils.createLogMsg(getClass(), "onPaging()");
			logger.error(logMsg, e);
			Msg.showError(e);
		}
	}
	
	private void doQuery(int activePage, PagingBean<CaseVo> pageBean, String pageBeanName, String flow) throws Exception{
		
		pageBean.setList(null);
		pageBean.setTotalSize(0);
		pageBean.setActivePage(activePage);
		caseService.findStatusReviewDetail(caseParam, pageBean, userProfile.getRoleNoList().get(0), flow);
		BindUtils.postNotifyChange(null, null, this, pageBeanName);
		
	}
	
	@Command("onOpenInsuranceTask")
	public void onOpenInsuranceTask(@BindingParam("item") CaseVo item) {
		try {
			
			HashMap<String, Object> arg = new HashMap<String, Object>();
			arg.put("caseVo", item);

			FirFunctionStatusVo firFunctionStatusVo = new FirFunctionStatusVo(CaseFunction.FUN_REVIEW_TASK, item.getStatus());
			arg.put("firFunctionStatusVo", firFunctionStatusVo);

			Window window = (Window) Executions.createComponents(ZulPage.InsuranceTaskMain.getUrl(), view, arg);
			window.doModal();

			EventListener<Event> listener = new EventListener<Event>() {
				@Override
				public void onEvent(Event event) throws Exception {
					initQueryReviewDetail();
				}
			};
			
			window.addEventListener(FirEvent.ON_CLOSE_WINDOW, listener);
			
		} catch (Exception e) {
			e.printStackTrace();
			String methodName = "onOpenInsuranceTask()";
			String logMsg = LogUtils.createLogMsg(getClass(), methodName);
			logger.error(logMsg, e);
			Msg.showError(e);
		}
	}
	
	@Command("onApprove")
	public void onApprove(){
		
		try{
		
			Map<String,List<CaseVo>> reviewList = new HashMap<String,List<CaseVo>>();
			reviewList.put("insTask", pagingBeanInsTask.getSelectedItems());
			reviewList.put("insTempTask", pagingBeanInsTempTask.getSelectedItems());
			reviewList.put("insAbortTask", pagingBeanInsAbortTask.getSelectedItems());
			reviewList.put("insExceptionTask", pagingBeanExceptionTask.getSelectedItems());
			
			SessionInfoBean sessionInfo = SessionUtil.getSessionInfoBean();
			UserProfileBean userProfile = SessionUtil.getUserProfile();
			
			caseService.approveProcessList(reviewList, sessionInfo, userProfile);
			
			this.initQueryReviewDetail();
			
			
			EventListener<Event> listener = new EventListener<Event>() {
				@Override
				public void onEvent(Event event) throws Exception {
					Events.postEvent(view, new Event(FirEvent.ON_CLOSE_WINDOW));
					view.detach();
				}
			};
			
			view.addEventListener(Events.ON_CLOSE, listener);
		
		} catch (Exception e) {
			e.printStackTrace();
			String methodName = "onApprove()";
			String logMsg = LogUtils.createLogMsg(getClass(), methodName);
			logger.error(logMsg, e);
			Msg.showError(e);
		}
	}

	public PagingBean<CaseVo> getPagingBeanInsTask() {
		return pagingBeanInsTask;
	}

	public void setPagingBeanInsTask(PagingBean<CaseVo> pagingBeanInsTask) {
		this.pagingBeanInsTask = pagingBeanInsTask;
	}

	public PagingBean<CaseVo> getPagingBeanInsTempTask() {
		return pagingBeanInsTempTask;
	}

	public void setPagingBeanInsTempTask(PagingBean<CaseVo> pagingBeanInsTempTask) {
		this.pagingBeanInsTempTask = pagingBeanInsTempTask;
	}

	public PagingBean<CaseVo> getPagingBeanInsAbortTask() {
		return pagingBeanInsAbortTask;
	}

	public void setPagingBeanInsAbortTask(PagingBean<CaseVo> pagingBeanInsAbortTask) {
		this.pagingBeanInsAbortTask = pagingBeanInsAbortTask;
	}

	public PagingBean<CaseVo> getPagingBeanExceptionTask() {
		return pagingBeanExceptionTask;
	}

	public void setPagingBeanExceptionTask(
			PagingBean<CaseVo> pagingBeanExceptionTask) {
		this.pagingBeanExceptionTask = pagingBeanExceptionTask;
	}

	public boolean isInsTaskVisible() {
		return insTaskVisible;
	}

	public void setInsTaskVisible(boolean insTaskVisible) {
		this.insTaskVisible = insTaskVisible;
	}

	public boolean isInsTempTaskVisible() {
		return insTempTaskVisible;
	}

	public void setInsTempTaskVisible(boolean insTempTaskVisible) {
		this.insTempTaskVisible = insTempTaskVisible;
	}

	public boolean isInsAbortTaskVisible() {
		return insAbortTaskVisible;
	}

	public void setInsAbortTaskVisible(boolean insAbortTaskVisible) {
		this.insAbortTaskVisible = insAbortTaskVisible;
	}

	public boolean isInsExceptionTaskVisible() {
		return insExceptionTaskVisible;
	}

	public void setInsExceptionTaskVisible(boolean insExceptionTaskVisible) {
		this.insExceptionTaskVisible = insExceptionTaskVisible;
	}
	
}
