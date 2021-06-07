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
package fr.paris.lutece.plugins.crm.modules.notifygru.util;

public class CrmNotifyGruConstants
{
    // REST CONSTANTS
    public static final String PLUGIN_NAME = "/crmnotifygru";
    public static final String API_PATH = "api";
    public static final String VERSION_PATH = "/v{" + CrmNotifyGruConstants.VERSION + "}";
    public static final String ID_PATH = "/{" + CrmNotifyGruConstants.ID + "}";
    public static final String VERSION = "version";
    public static final String ID = "id";
    public static final String NOTICATION_PATH = "/notification";
    public static final String DEMAND_TYPE_PATH = "/demandType";

    public static final int VERSION_1 = 1;

    // MESSAGES
    public static final String ERROR_NOT_FOUND_VERSION = "Version not found";
    public static final String MESSAGE_INVALID_USER_ID = "The user ID doesn't correspond to the user Id of the original Demand";
    public static final String MESSAGE_MISSING_DEMAND_ID = "Demand Id and Demand type Id are mandatory";
    public static final String MESSAGE_MISSING_USER_ID = "User connection id is mandatory";

    // STATUS MESSAGES
    public static final String STATUS_RECEIVED = "{ \"acknowledge\" : { \"status\": \"received\" } }";

    /** The Constant DEFAULT_INT. */
    public static final int DEFAULT_INT = -1;

}
