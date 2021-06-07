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
package fr.paris.lutece.plugins.crm.modules.notifygru.web.rs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.crm.business.demand.Demand;
import fr.paris.lutece.plugins.crm.business.demand.DemandStatusCRM;
import fr.paris.lutece.plugins.crm.business.demand.DemandType;
import fr.paris.lutece.plugins.crm.business.demand.DemandTypeHome;
import fr.paris.lutece.plugins.crm.business.notification.Notification;
import fr.paris.lutece.plugins.crm.business.user.CRMUser;
import fr.paris.lutece.plugins.crm.service.CRMService;
import fr.paris.lutece.plugins.crm.service.demand.DemandService;
import fr.paris.lutece.plugins.crm.modules.notifygru.util.CrmNotifyGruConstants;
import fr.paris.lutece.plugins.crm.service.demand.DemandStatusCRMService;
import fr.paris.lutece.plugins.crm.service.user.CRMUserService;
import fr.paris.lutece.plugins.rest.service.RestConstants;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.AbstractJsonResponse;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonUtil;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

@Path( RestConstants.BASE_PATH + CrmNotifyGruConstants.API_PATH + CrmNotifyGruConstants.VERSION_PATH + CrmNotifyGruConstants.PLUGIN_NAME )
public class CrmNotifyGruRestService
{
    private final Logger _logger = Logger.getLogger( RestConstants.REST_LOGGER );

    /**
     * Web Service methode which permit to store the notification flow into a data store
     * 
     * @param nVersion
     * @param strJson
     *            The JSON flow
     * @param request
     * @return The response
     */
    @PUT
    @Path( CrmNotifyGruConstants.NOTICATION_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response notifications( @PathParam( CrmNotifyGruConstants.VERSION ) Integer nVersion, String strJson, @Context HttpServletRequest request )
    {

        if ( nVersion == CrmNotifyGruConstants.VERSION_1 )
        {
            return storeNotificationV1( strJson, request.getLocale( ) );
        }

        _logger.error( CrmNotifyGruConstants.ERROR_NOT_FOUND_VERSION );

        return Response.status( Response.Status.NOT_FOUND )
                .entity( JsonUtil
                        .buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), CrmNotifyGruConstants.ERROR_NOT_FOUND_VERSION ) ) )
                .build( );
    }

    /**
     * store the notification
     * 
     * @param strJson
     * @return the json response message
     */
    private Response storeNotificationV1( String strJson, Locale locale )
    {
        try
        {
            // Format from JSON
            ObjectMapper mapper = new ObjectMapper( );
            mapper.configure( DeserializationFeature.UNWRAP_ROOT_VALUE, true );
            mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

            fr.paris.lutece.plugins.grubusiness.business.notification.Notification gruNotification = mapper.readValue( strJson,
                    fr.paris.lutece.plugins.grubusiness.business.notification.Notification.class );
            AppLogService.debug( "crm-notifygru / notification - Received strJson : " + strJson );

            return store( gruNotification, locale );

        }
        catch( JsonParseException ex )
        {
            return error( ex + " :" + ex.getMessage( ), Response.Status.INTERNAL_SERVER_ERROR, ex );
        }
        catch( JsonMappingException ex )
        {
            return error( ex + " :" + ex.getMessage( ), Response.Status.INTERNAL_SERVER_ERROR, ex );
        }
        catch( IOException ex )
        {
            return error( ex + " :" + ex.getMessage( ), Response.Status.INTERNAL_SERVER_ERROR, ex );
        }
        catch( NullPointerException ex )
        {
            return error( ex + " :" + ex.getMessage( ), Response.Status.INTERNAL_SERVER_ERROR, ex );
        }
    }

    /**
     * Stores a notification and the associated demand
     * 
     * @param notification
     *            the notification to store
     */
    private Response store( fr.paris.lutece.plugins.grubusiness.business.notification.Notification gruNotification, Locale locale )
    {
        AbstractJsonResponse jsonResponse;

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

            if ( storedUser.getIdCRMUser( ) != storedDemand.getIdCRMUser( ) )
            {
                return error( CrmNotifyGruConstants.MESSAGE_INVALID_USER_ID, Response.Status.PRECONDITION_FAILED, null );
            }

            // set modification date
            storedDemand.setDateModification( crmDemand.getDateModification( ) );

            // set status if it has changed (incorrect status will be ignored)
            if ( storedDemand.getIdStatusCRM( ) != crmDemand.getIdStatusCRM( ) && crmDemand.getIdStatusCRM( ) >= 0 )
            {
                DemandStatusCRM statusCRM = DemandStatusCRMService.getService( ).getStatusCRM( crmDemand.getIdStatusCRM( ), locale );

                if ( statusCRM != null )
                {
                    storedDemand.setIdStatusCRM( crmDemand.getIdStatusCRM( ) );
                    storedDemand.setStatusText( statusCRM.getLabel( ) );
                }
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

            // get status text
            if ( crmDemand.getIdStatusCRM( ) >= 0 )
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

        // success
        return Response.status( Response.Status.CREATED ).entity( CrmNotifyGruConstants.STATUS_RECEIVED ).build( );
    }

    /**
     * get crm Demand from the GRU Demand of the GRU notification
     * 
     * @param gruNotification
     * @return the demand
     */
    private Demand getCrmDemand( fr.paris.lutece.plugins.grubusiness.business.notification.Notification gruNotification )
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
        }

        return crmDemand;
    }

    /**
     * get CRM notification from GRU notification
     * 
     * @param gruNotification
     * @return the notification
     */
    private Notification getCrmNotification( fr.paris.lutece.plugins.grubusiness.business.notification.Notification gruNotification )
    {
        Notification crmNotification = new Notification( );

        if ( gruNotification.getMyDashboardNotification( ) != null )
        {
            crmNotification.setMessage( gruNotification.getMyDashboardNotification( ).getMessage( ) );
            crmNotification.setSender( gruNotification.getMyDashboardNotification( ).getSenderName( ) );
            crmNotification.setObject( gruNotification.getMyDashboardNotification( ).getSubject( ) );
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
    private Response error( String strMessage, Response.Status status, Throwable ex )
    {
        if ( ex != null )
        {
            AppLogService.error( strMessage, ex );
        }
        else
        {
            AppLogService.error( strMessage );
        }

        String strError = "{ \"status\": \"Error : " + strMessage + "\" }";

        return Response.status( status ).entity( strError ).build( );
    }
    
    /**
     * Web Service methode which permit to store the notification flow into a data store
     * 
     * @return The response
     */
    @GET
    @Path( CrmNotifyGruConstants.DEMAND_TYPE_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getDemandTypes( )
    {

        List<DemandType> listDemandTypes = DemandTypeHome.findAll( );
        try
        {
            ObjectMapper mapper = new ObjectMapper( );
            
            String strResult = mapper.writeValueAsString( listDemandTypes );
            return Response.ok( strResult ).build( );
        }
        catch( JsonProcessingException e )
        {
            return Response.serverError( ).build( );
        }

    }
}
