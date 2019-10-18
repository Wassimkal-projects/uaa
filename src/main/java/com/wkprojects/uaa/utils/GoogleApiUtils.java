/**
 * Copyright (c) 2019. CreatedBy Wassim KALBOUSSI.  All rights reserved.
 */

package com.wkprojects.uaa.utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.wkprojects.uaa.model.GoogleUserModel;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

public class GoogleApiUtils {

    private static final String CLIENT_ID = "196194089193-3ks6sacirmdorvbmsra2o43t745hdbtd.apps.googleusercontent.com";

    public static Optional<GoogleUserModel> checkIfGoogleTokenIsValid(String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            Payload payload = idToken.getPayload();

            GoogleUserModel googleUserModel = new GoogleUserModel();
            googleUserModel.setUserId(payload.getSubject());
            googleUserModel.setEmail(payload.getEmail());
            googleUserModel.setName((String) payload.get("name"));
            googleUserModel.setPictureUrl((String) payload.get("picture"));
            googleUserModel.setLocale((String) payload.get("locale"));
            googleUserModel.setFamilyName((String) payload.get("family_name"));
            googleUserModel.setGivenName((String) payload.get("given_name"));
            googleUserModel.setEmailVerified(payload.getEmailVerified());

            return Optional.of(googleUserModel);

        } else {
            return Optional.empty();
        }
    }
}
