/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

package rjc.jplanner.gui;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;

/*************************************************************************************************/
/*************** Extended version of SplitPane with preferred left-most node width ***************/
/*************************************************************************************************/

public class XSplitPane extends SplitPane
{
  public int      preferredLeftNodeWidth = 600;
  private boolean m_splitResize          = false;

  /**************************************** constructor ******************************************/
  public XSplitPane( Node... items )
  {
    // create enhanced split-pane
    super( items );

    // add listener to ensure divider is at preferred position when pane resized
    widthProperty().addListener( ( observable, oldWidth, newWidth ) ->
    {
      m_splitResize = true;
      setDividerPosition( 0, preferredLeftNodeWidth / newWidth.doubleValue() );
    } );

    // add listener to ensure preferred position is updated when divider manually moved
    getDividers().get( 0 ).positionProperty().addListener( ( observable, oldPos, newPos ) ->
    {
      if ( !m_splitResize )
        preferredLeftNodeWidth = (int) ( getWidth() * newPos.doubleValue() );
      m_splitResize = false;
    } );

  }

}
