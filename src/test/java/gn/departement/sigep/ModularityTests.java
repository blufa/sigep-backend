package gn.departement.sigep;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Vérifie que les modules (platform, common, iam, staff, badges, attendance) respectent leurs frontières. */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(SigepApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
