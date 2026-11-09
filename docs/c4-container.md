# C4 nivel Container: apertura de cuenta

```mermaid
flowchart LR
    cliente["Cliente HTTP"]
    orq["account-opening-orchestrator-service :8080\nSaga orquestada y estado de la saga"]
    cuenta["account-service :8081\nCuenta y comisión"]
    tarjeta["card-service :8082\nTarjeta"]
    dbOrq[("H2 saga")]
    dbCuenta[("H2 cuentas")]
    dbTarjeta[("H2 tarjetas")]

    cliente -->|"POST /api/v1/account-openings"| orq
    orq -->|"1. crear / revertir cuenta\ncircuit breaker accountService"| cuenta
    orq -->|"2. crear tarjeta\n3. cobrar comisión\nrevertir tarjeta\ncircuit breaker cardService"| tarjeta
    tarjeta -->|"cobrar o devolver comisión\ncircuit breaker accountService"| cuenta
    orq --- dbOrq
    cuenta --- dbCuenta
    tarjeta --- dbTarjeta
```

La compensación recorre el camino inverso: el orquestador pide el reverso de la tarjeta y después el reverso de la cuenta. El reverso de la tarjeta devuelve la comisión cuando el cobro ya se había aplicado. No hay bus de eventos.
