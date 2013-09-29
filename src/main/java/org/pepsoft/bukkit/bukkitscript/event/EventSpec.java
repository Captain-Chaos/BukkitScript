/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.event;

import java.util.Properties;

/**
 *
 * @author pepijn
 */
public class EventSpec {
    public EventSpec(String eventDescriptor, String scriptName, Properties context) {
        this.eventDescriptor = eventDescriptor;
        this.scriptName = scriptName;
        this.context = context;
    }

    public Properties getContext() {
        return context;
    }

    public String getEventDescriptor() {
        return eventDescriptor;
    }

    public String getScriptName() {
        return scriptName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EventSpec other = (EventSpec) obj;
        if ((this.eventDescriptor == null) ? (other.eventDescriptor != null) : !this.eventDescriptor.equals(other.eventDescriptor)) {
            return false;
        }
        if ((this.scriptName == null) ? (other.scriptName != null) : !this.scriptName.equals(other.scriptName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.eventDescriptor != null ? this.eventDescriptor.hashCode() : 0);
        hash = 41 * hash + (this.scriptName != null ? this.scriptName.hashCode() : 0);
        return hash;
    }
    private final String eventDescriptor;
    private final String scriptName;
    private final Properties context;
}