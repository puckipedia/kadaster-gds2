kadaster-gds2
=============

JAX-WS classes voor benaderen van de Generieke Download Service 2 van het Kadaster. 

## Schemas

De bestanden in de schemas/ directory zijn afkomstig van het Kadaster.

## Maken keystore met PKIoverheid certificaat

Je hebt een PKIoverheid certificaat nodig waarvan het publieke deel bij Kadaster bekend is. Hiermee word je geauthenticeerd en aan de juiste afgiftes gekoppeld aan de serverkant bij Kadaster.

Dit certificaat moet worden geimporteerd in een keystore (src/main/resources/gds2_key.jks). Uitgaande van PEM encoded certificaat en key, doe als volgt:

Maken PKCS12 bestand met certificaat en key. Verifieert ook dat key en certificaat overeenkomen.
`openssl pkcs12 -export -out -secure mycert.p12 -inkey mykey.key -in mycert.crt -name mycert`

Importeren in keystore:
`keytool -importkeystore -destkeystore src/main/resources/gds2_key.jks -srckeystore mycert.p12 -srcstoretype pkcs12 -alias mycert`

Gebruik als wachtwoord "changeit".
