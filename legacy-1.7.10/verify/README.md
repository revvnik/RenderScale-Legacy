# Transformer verification

`TransformerVerifier.java` applies the transformer to the obfuscated Minecraft
1.7.10 `EntityRenderer` class (`blt.class`) and checks the inserted hooks.

The Minecraft class itself is intentionally not distributed. The verifier was
run against an original 1.7.10 client class while producing the release JAR.
