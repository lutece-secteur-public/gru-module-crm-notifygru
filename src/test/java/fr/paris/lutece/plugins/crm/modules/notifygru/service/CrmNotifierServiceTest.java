/*
 * Copyright (c) 2002-2026, City of Paris
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.crm.modules.notifygru.util.CrmNotifyGruConstants;
import fr.paris.lutece.plugins.grubusiness.business.customer.Customer;
import fr.paris.lutece.plugins.grubusiness.business.demand.Demand;
import fr.paris.lutece.plugins.grubusiness.business.notification.Notification;
import fr.paris.lutece.plugins.grubusiness.business.notification.NotifyGruResponse;
import fr.paris.lutece.plugins.grubusiness.service.notification.INotifierServiceProvider;
import fr.paris.lutece.plugins.grubusiness.service.notification.NotificationException;
import fr.paris.lutece.test.LuteceTestCase;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

/**
 * CrmNotifierService tests
 */
public class CrmNotifierServiceTest extends LuteceTestCase
{
    @Inject
    private CrmNotificationService _crmNotificationService;

    /**
     * The local notifier is resolved by its bean name and is disabled by default
     */
    @Test
    public void testNotifierBeanByName( )
    {
        INotifierServiceProvider notifier = CDI.current( ).select( INotifierServiceProvider.class, NamedLiteral.of( CrmNotifierService.BEAN_NAME ) ).get( );

        assertTrue( notifier instanceof CrmNotifierService, "named bean must be the CRM notifier" );
        assertFalse( notifier.isEnabled( ), "notifier must be disabled by default" );
    }

    /**
     * A notification without dashboard part is ignored
     *
     * @throws NotificationException
     *             the notification exception
     */
    @Test
    public void testProcessWithoutDashboardNotification( ) throws NotificationException
    {
        INotifierServiceProvider notifier = CDI.current( ).select( INotifierServiceProvider.class, NamedLiteral.of( CrmNotifierService.BEAN_NAME ) ).get( );

        assertNull( notifier.process( new Notification( ) ), "notification without dashboard part must be ignored" );
    }

    /**
     * A notification without connection id is refused
     */
    @Test
    public void testStoreWithoutConnectionId( )
    {
        Notification notification = new Notification( );
        notification.setDemand( new Demand( ) );

        NotifyGruResponse response = _crmNotificationService.store( notification, null );

        assertEquals( NotifyGruResponse.STATUS_ERROR, response.getStatus( ), "status" );
        assertEquals( CrmNotifyGruConstants.MESSAGE_MISSING_USER_ID, response.getErrors( ).get( 0 ).getMessage( ), "error message" );
    }

    /**
     * A notification with a non numeric demand type is refused
     */
    @Test
    public void testStoreWithIncorrectDemandType( )
    {
        Customer customer = new Customer( );
        customer.setConnectionId( "connection-id" );
        Demand demand = new Demand( );
        demand.setCustomer( customer );
        demand.setId( "remote-id" );
        demand.setTypeId( "type" );
        Notification notification = new Notification( );
        notification.setDemand( demand );

        NotifyGruResponse response = _crmNotificationService.store( notification, null );

        assertEquals( NotifyGruResponse.STATUS_ERROR, response.getStatus( ), "status" );
        assertEquals( CrmNotifyGruConstants.MESSAGE_INCORRECT_DEMAND_ID, response.getErrors( ).get( 0 ).getMessage( ), "error message" );
    }
}
