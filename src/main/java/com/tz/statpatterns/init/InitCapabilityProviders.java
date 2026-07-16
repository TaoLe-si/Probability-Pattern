package com.tz.statpatterns.init;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.core.definitions.AEBlockEntities;
import com.tz.statpatterns.core.definition.SPBlockEntities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class InitCapabilityProviders {
    private InitCapabilityProviders() {
    }
    public static void register(RegisterCapabilitiesEvent event) {
        initPatternProvider(event);
    }
    private static void initPatternProvider(RegisterCapabilitiesEvent event) {

    }
}
