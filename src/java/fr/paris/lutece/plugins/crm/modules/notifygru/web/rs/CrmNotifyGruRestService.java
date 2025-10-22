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

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.crm.business.demand.DemandType;
import fr.paris.lutece.plugins.crm.business.demand.DemandTypeHome;
import fr.paris.lutece.plugins.crm.modules.notifygru.service.CrmNotificationService;
import fr.paris.lutece.plugins.crm.modules.notifygru.util.CrmNotifyGruConstants;
import fr.paris.lutece.plugins.grubusiness.business.notification.NotifyGruResponse;
import fr.paris.lutece.plugins.rest.service.RestConstants;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

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
	NotifyGruResponse gruResponse;
	
        if ( nVersion == CrmNotifyGruConstants.VERSION_1 )
        {
            gruResponse = storeNotificationV1( strJson, I18nService.getDefaultLocale( ) );
            
            return Response.status( NotifyGruResponse.STATUS_ERROR.equals ( gruResponse.getStatus( ) )
        	    ?Response.Status.BAD_REQUEST:Response.Status.CREATED )
                    .entity( gruResponse )
                    .build( );
        }
        else
        {
            gruResponse = CrmNotificationService.error ( CrmNotifyGruConstants.ERROR_NOT_FOUND_VERSION, 
        	    Response.Status.NOT_FOUND, null);
            
            _logger.error( CrmNotifyGruConstants.ERROR_NOT_FOUND_VERSION );
            
            return Response.status( Response.Status.NOT_FOUND )
                    .entity( gruResponse )
                    .build( );
        }
    }

    /**
     * store the notification
     * 
     * @param strJson
     * @return the json response message
     */
    private NotifyGruResponse storeNotificationV1( String strJson, Locale locale )
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

            return CrmNotificationService.store( gruNotification, locale );
        }
        catch ( JsonProcessingException ex )
        {
            return CrmNotificationService.error( ex + " :" + ex.getMessage( ), Response.Status.BAD_REQUEST, ex );
        }
        catch ( Exception ex )
        {
            return CrmNotificationService.error( ex + " :" + ex.getMessage( ), Response.Status.INTERNAL_SERVER_ERROR, ex );
        }
        
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
