/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.plan.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rjc.jplanner.plan.resources.Resource.FIELD;

/*************************************************************************************************/
/*************** Maps resource tag strings to list of corresponding plan resources ***************/
/*************************************************************************************************/

class ResourceLookup
{
  private static final int            INITIALS   = FIELD.Initials.ordinal();
  private static final int            NAME       = FIELD.Name.ordinal();
  private static final int            ORG        = FIELD.Organisation.ordinal();
  private static final int            GROUP      = FIELD.Group.ordinal();
  private static final int            ROLE       = FIELD.Role.ordinal();
  private static final int            ALIAS      = FIELD.Alias.ordinal();

  private static final int[]          TAG_FIELDS = { INITIALS, NAME, ORG, GROUP, ROLE, ALIAS };

  private Map<String, List<Resource>> m_resourcesByTag;

  /***************************************** findByTag ******************************************/
  public List<Resource> findByTag( Resources resources, String tag )
  {
    // exact, case-sensitive lookup of resource tag strings
    if ( tag == null || tag.isBlank() )
      return List.of();

    if ( m_resourcesByTag == null )
      m_resourcesByTag = buildByTag( resources );

    return m_resourcesByTag.getOrDefault( tag, List.of() );
  }

  /**************************************** invalidate ******************************************/
  public void invalidate()
  {
    m_resourcesByTag = null;
  }

  /**************************************** buildByTag ******************************************/
  private static Map<String, List<Resource>> buildByTag( Resources resources )
  {
    // build immutable lookup map, skipping resource 0 and blank tag fields
    var build = new HashMap<String, ArrayList<Resource>>();
    for ( int index = 1; index < resources.size(); index++ )
    {
      var resource = resources.get( index );
      if ( !resource.isBlank() )
        for ( int field : TAG_FIELDS )
          addTag( build, resource, field );
    }

    var result = new HashMap<String, List<Resource>>( build.size() );
    build.forEach( ( tag, found ) -> result.put( tag, List.copyOf( found ) ) );
    return Map.copyOf( result );
  }

  /****************************************** addTag *********************************************/
  private static void addTag( Map<String, ArrayList<Resource>> build, Resource resource, int field )
  {
    // add one tag string to the build map, collapsing duplicate resource matches
    if ( resource.getValue( field ) instanceof String tag && !tag.isBlank() )
    {
      var resources = build.computeIfAbsent( tag, key -> new ArrayList<>() );
      if ( !resources.contains( resource ) )
        resources.add( resource );
    }
  }

  /***************************************** isTagField ******************************************/
  public static boolean isTagField( int field )
  {
    for ( int tagField : TAG_FIELDS )
      if ( field == tagField )
        return true;

    return false;
  }

}