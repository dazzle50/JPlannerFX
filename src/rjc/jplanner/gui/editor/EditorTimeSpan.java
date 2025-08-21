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

package rjc.jplanner.gui.editor;

import rjc.jplanner.gui.TimeSpanField;
import rjc.table.view.editor.AbstractCellEditor;

/*************************************************************************************************/
/******************************* Table cell editor for Time-Spans ********************************/
/*************************************************************************************************/

public class EditorTimeSpan extends AbstractCellEditor
{
  TimeSpanField m_editor = new TimeSpanField(); // time-span editor

  /**************************************** constructor ******************************************/
  public EditorTimeSpan()
  {
    // create time-span table cell editor
    setControl( m_editor );
  }

  /******************************************* getValue ******************************************/
  @Override
  public Object getValue()
  {
    // get editor time-span value
    return m_editor.getTimeSpan();
  }

  /******************************************* setValue ******************************************/
  @Override
  public void setValue( Object value )
  {
    // set value depending on type
    if ( value == null )
      m_editor.setValue( "1d" );
    else
      m_editor.setValue( value );
  }

  /******************************************* isValueValid ******************************************/
  @Override
  public boolean isValueValid( Object value )
  {
    // allow any value to start the editing process
    return true;
  }
}
