package gn.departement.sigep.iam.adapters.in.web.dto;

import java.util.List;

/** {@code menuKeys} null = aucune restriction ; liste (même vide) = restriction exacte à ces clés. */
public record UpdateUserMenusRequest(List<String> menuKeys) {
}
