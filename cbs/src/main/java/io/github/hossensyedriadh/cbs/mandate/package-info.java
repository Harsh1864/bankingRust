@ApplicationModule(
        allowedDependencies = {"exception", "configuration :: mapper", "account :: service", "account :: model", "account :: enumerator", "transaction :: service", "transaction :: command", "transaction :: model", "eod :: service"}
)
package io.github.hossensyedriadh.cbs.mandate;

import org.springframework.modulith.ApplicationModule;
