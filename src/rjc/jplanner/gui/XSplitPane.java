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
/*************** Extended version of SplitPane with preferred first split position ***************/
/*************************************************************************************************/

public class XSplitPane extends SplitPane
{
  private int     m_preferredDividerPosition = 750;
  private boolean m_splitResize;

  /***************************************** constructor *****************************************/
  public XSplitPane( Node... items )
  {
    // create enhanced split-pane
    super( items );

    // add listener to ensure divider is at preferred position when pane resized
    widthProperty().addListener( ( property, oldWidth, newWidth ) ->
    {
      m_splitResize = true;
      setDividerPosition( 0, m_preferredDividerPosition / newWidth.doubleValue() );
    } );

    // add listener to ensure preferred position is updated when divider manually moved
    getDividers().get( 0 ).positionProperty().addListener( ( property, oldPos, newPos ) ->
    {
      if ( !m_splitResize )
        m_preferredDividerPosition = (int) ( getWidth() * newPos.doubleValue() );
      m_splitResize = false;
    } );
  }

  /********************************* getPreferredDividerPosition *********************************/
  public int getPreferredDividerPosition()
  {
    // return preferred divider position
    return m_preferredDividerPosition;
  }

  /********************************* setPreferredDividerPosition *********************************/
  public void setPreferredDividerPosition( int position )
  {
    // set preferred divider position
    m_preferredDividerPosition = position;
  }

}
