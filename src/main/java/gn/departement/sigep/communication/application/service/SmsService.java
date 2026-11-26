package gn.departement.sigep.communication.application.service;

import gn.departement.sigep.communication.application.port.in.SmsUseCases;
import gn.departement.sigep.communication.application.port.out.SmsGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService implements SmsUseCases {

    private final SmsGateway gateway;

    public SmsService(SmsGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("[SMS] destinataire manquant, SMS non envoyé : {}", message);
            return;
        }
        SmsGateway.DeliveryResult result = gateway.send(phoneNumber, message);
        if (!result.delivered()) {
            log.warn("[SMS] échec d'envoi vers {} : {}", phoneNumber, result.detail());
        }
    }
}
