# MineFables — Vértice Vazio: recast da Reescrita Dracônica (v10)

Patch incremental **sobre a v9** (`MineFables_Zona_Morte_120s_FireResistance_v9.zip`).

## Mudança

Quando William, nível 6, é o conjurador e permanece dentro do Vértice Vazio, usar novamente a habilidade **Reescrita Dracônica** deixa de iniciar a ultimate. O recast chama o encerramento já existente em `WilliamZone.exitGroup`, que devolve as entidades da sessão para as posições originais e aplica o cooldown especial de **500 segundos** a partir da saída antecipada.

A habilidade continua funcionando normalmente fora da Zona, após o fim do cooldown. Participantes não conjuradores não podem encerrar a sessão pelo recast de William. Não foram alterados duração de 120 segundos, proteção de queda, Resistência ao Fogo, animações, HUD, Fulgor, NBT ou regras de saída central.

## Instalação

Na raiz do MineFables, após a v9:

```powershell
Expand-Archive -Path "$env:USERPROFILE\Downloads\MineFables_Zona_Recast_Encerra_v10.zip" -DestinationPath . -Force
.\gradlew.bat compileJava
.\gradlew.bat runClient
```

Não é necessário reinstalar a estrutura.

## Testes

1. Entre na Zona com William e pelo menos um participante; antes dos 120 segundos, selecione **Reescrita Dracônica** novamente (a habilidade normal, pois a ultimate já terminou) e ative-a.
2. Ambos devem voltar às posições/dimensões de origem, sem reacender o Dragon Override ou reiniciar sua duração.
3. Confira que Reescrita Dracônica entra em cooldown de **500 segundos a partir do encerramento**, impedindo novo uso nesse intervalo.
4. Depois da recarga, a ultimate deve funcionar normalmente fora da Zona.
5. Teste também o encerramento por duração, pela saída central e pela morte; nenhum desses fluxos foi alterado.

## Validação

As duas classes modificadas passaram na análise sintática Java 21. A compilação Gradle completa e os testes dentro do jogo ainda devem ser realizados no PC com as dependências instaladas.
