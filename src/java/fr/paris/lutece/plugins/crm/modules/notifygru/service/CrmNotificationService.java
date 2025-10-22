/*
 * Copyright (c) 2002-2021, City of Paris
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
package fr.paris.lutece.plugins.crm.modules.notifygru.service;


import java.sql.Timestamp;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.crm.business.demand.Demand;
import fr.paris.lutece.plugins.crm.business.demand.DemandStatusCRM;
import fr.paris.lutece.plugins.crm.business.notification.Notification;
import fr.paris.lutece.plugins.crm.business.user.CRMUser;
import fr.paris.lutece.plugins.crm.modules.notifygru.util.CrmNotifyGruConstants;
import fr.paris.lutece.plugins.crm.service.CRMService;
import fr.paris.lutece.plugins.crm.service.demand.DemandService;
import fr.paris.lutece.plugins.crm.service.demand.DemandStatusCRMService;
import fr.paris.lutece.plugins.crm.service.demand.DemandTypeService;
import fr.paris.lutece.plugins.crm.service.user.CRMUserService;
import fr.paris.lutece.plugins.grubusiness.business.notification.Event;
import fr.paris.lutece.plugins.grubusiness.business.notification.NotifyGruResponse;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.AbstractJsonResponse;

public class CrmNotificationService
{
    private final static String CHARECTER_REGEXP_FILTER = "[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\p{Sm}\\p{Sc}\\s]";


    /**
     * Stores a notification and the associated demand
     * 
     * @param notification
     *            the notification to store
     */
    public static NotifyGruResponse store( fr.paris.lutece.plugins.grubusiness.business.notification.Notification gruNotification, Locale locale )
    {    
        // check if connection id is present
        if ( gruNotification.getDemand( ) == null || gruNotification.getDemand( ).getCustomer( ) == null
                || StringUtils.isBlank( gruNotification.getDemand( ).getCustomer( ).getConnectionId( ) ) )
        {
            return error( CrmNotifyGruConstants.MESSAGE_MISSING_USER_ID, Response.Status.PRECONDITION_FAILED, null );

        }

        // check if Demand remote id and demand type id are present
        if ( StringUtils.isBlank( gruNotification.getDemand( ).getId( ) ) || StringUtils.isBlank( gruNotification.getDemand( ).getTypeId( ) ) )
        {
            return error( CrmNotifyGruConstants.MESSAGE_MISSING_DEMAND_ID, Response.Status.PRECONDITION_FAILED, null );
        }

        // check id demand_type_id is numeric
        if ( !StringUtils.isNumeric( gruNotification.getDemand( ).getTypeId( ) ) )
        {
        	return error( CrmNotifyGruConstants.MESSAGE_INCORRECT_DEMAND_ID, Response.Status.PRECONDITION_FAILED, null );
        }
        
        int demandTypeId = Integer.parseInt( gruNotification.getDemand( ).getTypeId( ) );
        
        // check if  demand type id exists
        if ( DemandTypeService.getService().findByPrimaryKey( demandTypeId  ) == null ) 
        {
            return error( CrmNotifyGruConstants.MESSAGE_INCORRECT_DEMAND_ID, Response.Status.PRECONDITION_FAILED, null );
        }

        // get CRM demand from GRU Demand
        Demand crmDemand = getCrmDemand( gruNotification );

        // check if crm Demand already exists
        Demand storedDemand = DemandService.getService( ).findByRemoteKey( crmDemand.getRemoteId( ), crmDemand.getIdDemandType( ) );
        int demandId = -1;

        if ( storedDemand != null )
        {
            demandId = storedDemand.getIdDemand( );

            // check if the customer id still the same
            CRMUser storedUser = CRMUserService.getService( ).findByUserGuid( gruNotification.getDemand( ).getCustomer( ).getConnectionId( ) );

            if ( storedUser != null && ( storedUser.getIdCRMUser( ) != storedDemand.getIdCRMUser( ) ) )
            {
                return error( CrmNotifyGruConstants.MESSAGE_INVALID_USER_ID, Response.Status.PRECONDITION_FAILED, null );
            }

            // set modification date
            storedDemand.setDateModification( crmDemand.getDateModification( ) );

            // set status id if updated
            if ( storedDemand.getIdStatusCRM( ) != crmDemand.getIdStatusCRM( ) && crmDemand.getIdStatusCRM( ) >= 0 )
            {
            	storedDemand.setIdStatusCRM( crmDemand.getIdStatusCRM( ) );
            }
            
            // set status text if updated
            if ( crmDemand.getStatusText( )!=null && !crmDemand.getStatusText( ).equals(storedDemand.getStatusText( ) ) )
            {
            	storedDemand.setStatusText( crmDemand.getStatusText( ) );
            }
            
            // set status label if not set
            if ( StringUtils.isBlank( storedDemand.getStatusText( ) ) )
            {
            	DemandStatusCRM statusCRM = DemandStatusCRMService.getService( ).getStatusCRM( crmDemand.getIdStatusCRM( ), locale );
            	storedDemand.setStatusText( statusCRM.getLabel( ) );
            }  
      

            // update stored demand
            DemandService.getService( ).update( storedDemand );
        }
        else
        {
            // create user if not exists in CRM database
            int nUserId;
            CRMUser storedUser = CRMUserService.getService( ).findByUserGuid( gruNotification.getDemand( ).getCustomer( ).getConnectionId( ) );
            if ( storedUser != null )
            {
                nUserId = storedUser.getIdCRMUser( );
            }
            else
            {
                CRMUser crmUser = new CRMUser( );
                crmUser.setUserGuid( gruNotification.getDemand( ).getCustomer( ).getConnectionId( ) );
                crmUser.setMustBeUpdated( true );

                nUserId = CRMUserService.getService( ).create( crmUser );
            }

            crmDemand.setIdCRMUser( nUserId );

            // get status text if not set in notif
            if ( StringUtils.isBlank( crmDemand.getStatusText() ) && crmDemand.getIdStatusCRM( ) >= 0 )
            {
                DemandStatusCRM statusCRM = DemandStatusCRMService.getService( ).getStatusCRM( crmDemand.getIdStatusCRM( ), locale );
                if ( statusCRM != null )
                {
                    crmDemand.setStatusText( statusCRM.getLabel( ) );
                }
            }

            // create demand
            demandId = DemandService.getService( ).create( crmDemand );
        }

        // get CRM notification from GRU notification
        Notification crmNotification = getCrmNotification( gruNotification );

        CRMService.getService( ).notify( demandId, crmNotification.getObject( ), crmNotification.getMessage( ), crmNotification.getSender( ) );

        NotifyGruResponse response = new NotifyGruResponse ( );
        response.setStatus ( NotifyGruResponse.STATUS_RECEIVED );
        
        // success
        return response;
    }

    /**
     * get crm Demand from the GRU Demand of the GRU notification
     * 
     * @param gruNotification
     * @return the demand
     */
    private static Demand getCrmDemand( fr.paris.lutece.plugins.grubusiness.business.notification.Notification gruNotification )
    {
        Demand crmDemand = new Demand( );

        if ( gruNotification.getDemand( ) != null )
        {
            crmDemand.setRemoteId( gruNotification.getDemand( ).getId( ) );
            crmDemand.setIdStatusCRM( gruNotification.getDemand( ).getStatusId( ) );
            crmDemand.setDateModification( new Timestamp( gruNotification.getDate( ) ) );
            crmDemand.setData( StringUtils.EMPTY );

            if ( StringUtils.isNumeric( gruNotification.getDemand( ).getTypeId( ) ) )
            {
                crmDemand.setIdDemandType( Integer.parseInt( gruNotification.getDemand( ).getTypeId( ) ) );
            }
            
            // update  demand status from mydashboard
            if ( gruNotification.getMyDashboardNotification( ).getStatusText( ) != null ) 
            {
            	crmDemand.setStatusText( gruNotification.getMyDashboardNotification( ).getStatusText( ) );
            }
            if ( gruNotification.getMyDashboardNotification( ).getStatusId( ) >= 0 ) 
            {
            	crmDemand.setIdStatusCRM( gruNotification.getMyDashboardNotification( ).getStatusId( ) );
            }
        }

        return crmDemand;
    }

    /**
     * get CRM notification from GRU notification
     * 
     * @param gruNotification
     * @return the notification
     */
    public static Notification getCrmNotification( fr.paris.lutece.plugins.grubusiness.business.notification.Notification gruNotification )
    {
        Notification crmNotification = new Notification( );

        if ( gruNotification.getMyDashboardNotification( ) != null )
        {
        	// clean message (avoir emoticons...s)
            crmNotification.setMessage( gruNotification.getMyDashboardNotification( ).getMessage( ).replaceAll(CHARECTER_REGEXP_FILTER,"") );
            
            crmNotification.setSender( gruNotification.getMyDashboardNotification( ).getSenderName( ) );
            
            // clean subject
            crmNotification.setObject( gruNotification.getMyDashboardNotification( ).getSubject( ).replaceAll(CHARECTER_REGEXP_FILTER,"") );
        }

        return crmNotification;
    }

    /**
     * Build an error response
     * 
     * @param strMessage
     *            The error message
     * @param ex
     *            An exception
     * @return The response
     */
    public static NotifyGruResponse error( String strMessage, Response.Status status, Throwable ex )
    {
        if ( ex != null )
        {
            AppLogService.error( strMessage, ex );
        }
        else
        {
            AppLogService.error( strMessage );
        }

        NotifyGruResponse response = new NotifyGruResponse ( );
        response.setStatus ( NotifyGruResponse.STATUS_ERROR );
        Event error = new Event( );
        error.setMessage ( strMessage );
        error.setStatus ( NotifyGruResponse.STATUS_ERROR );
        response.getErrors ( ).add ( error );

        return response;
    }
    
}
