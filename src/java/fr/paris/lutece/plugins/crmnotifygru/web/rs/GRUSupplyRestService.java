/*
 * Copyright (c) 2002-2019, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.crmnotifygru.web.rs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.crm.business.demand.Demand;
import fr.paris.lutece.plugins.crm.business.demand.DemandHome;
import fr.paris.lutece.plugins.crm.business.notification.Notification;
import fr.paris.lutece.plugins.crm.modules.rest.rs.CRMRest;
import fr.paris.lutece.plugins.crm.modules.rest.util.constants.CRMRestConstants;
import fr.paris.lutece.plugins.crm.service.CRMService;
import fr.paris.lutece.plugins.crm.service.demand.DemandService;
import fr.paris.lutece.plugins.crmnotifygru.constant.CrmGruConstants;
import fr.paris.lutece.plugins.rest.service.RestConstants;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.AbstractJsonResponse;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;

@Path( RestConstants.BASE_PATH + CrmGruConstants.PLUGIN_NAME )
public class GRUSupplyRestService
{
    /**
	 * notify a demand using Remote id and id demand Type and create the Demand if it doesn't exist
	 *
	 * @param nVersion
	 *            the API version
	 * @param strRemoteId
	 *            the Remote Id
	 * @param strIdDemandType
	 *            the demand type id
	 * @param strNotificationObject
	 *            the notification object
	 * @param strNotificationMessage
	 *            the notification message
	 * @param strNotificationSender
	 *            the sender
	 * @return the id demand
	 */
	@POST
	@Path( "/{version}/notification" )
	@Produces( MediaType.APPLICATION_JSON )
	@Consumes( MediaType.APPLICATION_JSON )
	public String doCreateDemandNotify( @PathParam( CRMRestConstants.API_VERSION ) Integer nVersion,
			@FormParam( CRMRestConstants.PARAMETER_REMOTE_ID ) String strRemoteId,
			@FormParam( CRMRestConstants.PARAMETER_ID_DEMAND_TYPE ) String strIdDemandType,
			@FormParam( CRMRestConstants.PARAMETER_USER_GUID ) String strUserGuid,
			@FormParam( CRMRestConstants.PARAMETER_ID_STATUS_CRM ) String strIdStatusCRM,
			@FormParam( CRMRestConstants.PARAMETER_STATUS_TEXT ) String strStatusText,
			@FormParam( CRMRestConstants.PARAMETER_DEMAND_DATA ) String strData,
			@Context HttpServletRequest request)
	{
		AbstractJsonResponse jsonResponse;
		CRMRest crmRest = new CRMRest( );
		if ( nVersion == CrmGruConstants.VERSION_1 )
		{
			if ( StringUtils.isNotBlank( strRemoteId ) && StringUtils.isNumeric( strIdDemandType ) && StringUtils.isNotBlank( strData ) )
			{

				int nIdDemandType = Integer.parseInt( strIdDemandType );
				Demand demand = DemandService.getService( ).findByRemoteKey( strRemoteId, nIdDemandType );

				if ( demand != null )
				{
					if( demand.getRemoteId( ).equals( strUserGuid ) )
					{   
						jsonResponse = notifyDemand( demand, strData);
					}
					else
					{
						AppLogService.error( CRMRestConstants.MESSAGE_CRM_REST + CrmGruConstants.MESSAGE_INVALID_USER_OR_DEMAND );
						jsonResponse = new ErrorJsonResponse( 
								String.valueOf(org.apache.commons.httpclient.HttpStatus.SC_PRECONDITION_FAILED), 
								CrmGruConstants.MESSAGE_INVALID_USER_OR_DEMAND ) ;
					}
				}
				else
				{
					
					demand =  DemandService.getService( ).findByPrimaryKey( Integer.parseInt( crmRest.doCreateDemandByUserGuidV2(nIdDemandType, strRemoteId, strIdDemandType, strUserGuid, strIdStatusCRM, strStatusText, strData, request) ) );
					jsonResponse = notifyDemand( demand, strData);
				}

			}
			else
			{
				AppLogService.error( CRMRestConstants.MESSAGE_CRM_REST + CRMRestConstants.MESSAGE_INVALID_DEMAND );
				jsonResponse = new ErrorJsonResponse( 
						String.valueOf(org.apache.commons.httpclient.HttpStatus.SC_PRECONDITION_FAILED), 
						CRMRestConstants.MESSAGE_INVALID_DEMAND ) ;
			}

		}
		else
		{
			AppLogService.error( CRMRestConstants.MESSAGE_CRM_REST + CRMRestConstants.MESSAGE_INVALID_API_VERSION );
			jsonResponse = new ErrorJsonResponse( 
					String.valueOf(org.apache.commons.httpclient.HttpStatus.SC_PRECONDITION_FAILED), 
					CRMRestConstants.MESSAGE_INVALID_API_VERSION ) ;
		}

		return JsonUtil.buildJsonResponse( jsonResponse );
	}
	
	private AbstractJsonResponse notifyDemand(Demand demand, String strData)
	{
		String strIdDemand = Integer.toString( demand.getIdDemand( ) );
		int strIdStatus = demand.getIdStatusCRM( );
		ObjectMapper mapper = new ObjectMapper( );
        mapper.configure( DeserializationFeature.UNWRAP_ROOT_VALUE, true );
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        Notification notification = new Notification( );
        
        try
        {
			notification = mapper.readValue( strData, Notification.class );
		}
        catch (JsonProcessingException e)
        {
        	return new JsonResponse( e + " :" + e.getMessage( ) );
		}
		CRMService.getService( ).notify( demand.getIdDemand( ), notification.getObject( ) , notification.getMessage( ), notification.getSender( ) );
		if( strIdStatus != demand.getIdStatusCRM( ) ) {
			DemandHome.update( demand );
		}
		// success
		return new JsonResponse( strIdDemand ) ;
	}

}
