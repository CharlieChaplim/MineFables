# MineFables — Zona Fabular v9 (morte, duração e resistências)

Patch incremental para o MineFables **já atualizado até William VFX/Zona v8**. Não inclui nem altera a estrutura NBT, o modelo da Fulgor, o círculo do Impétuo ou a HUD.

## Mudanças

1. **Morte dentro da Zona**: ao morrer, qualquer participante deixa de pertencer à sessão. Se o **conjurador** morrer, a Zona termina, participantes sobreviventes retornam à dimensão de origem, o bloqueio global da sessão é liberado e a ultimate recebe **500 s de cooldown**, iniciados na morte. O tempo de recarga é copiado para o jogador renascido.
2. **Resiliência**: verificações complementares no `PlayerEvent.Clone` e no respawn para recuperar sessões que, por alguma integração externa, não foram limpas na morte. No encerramento normal, o cooldown é atribuído ao conjurador pelo UUID, mesmo quando outro jogador acionou a saída.
3. **Duração da Zona**: **2 minutos (2400 ticks)**. A ultimate continua encerrando na conjuração; o cooldown especial de 500 s começa **no encerramento da Zona** (ou na morte do conjurador).
4. **Dano de queda**: só o conjurador dentro da Zona tem o dano de queda anulado. Dano de ataque, explosão e outros continuam normais. O centro X/Z de -5 a +5 e o resgate em Y<18 não mudaram.
5. **Resistência ao Fogo**: a Reescrita Dracônica aplica Resistência ao Fogo imediatamente e a renova a cada segundo enquanto a ultimate estiver ativa. Efeitos de poção mais duradouros são preservados; a proteção aplicada pelo mod pode persistir por no máximo cerca de dois segundos após o fim da ultimate.

## Instalação

Faça backup/commit do estado atual. Extraia o conteúdo deste ZIP na raiz do repositório e permita substituir os três arquivos Java correspondentes.

```powershell
Expand-Archive -Path "$env:USERPROFILE\Downloads\MineFables_Zona_Morte_120s_FireResistance_v9.zip" -DestinationPath . -Force
.\gradlew.bat compileJava
.\gradlew.bat runClient
```

Não reinstale o NBT: a arena atual é reutilizada.

## Testes

- [ ] Com William no nível 6, conjure a ultimate e entre na Zona. Confira que a saída por tempo ocorre após **120 segundos**.
- [ ] Dentro da Zona, cause a morte de William. Após renascer, ele não deve estar preso na sessão e a ultimate deve estar em cooldown de **500 segundos**, contado da morte. Após o tempo terminar, poderá entrar de novo.
- [ ] Entre com William e outro jogador; mate William. O outro jogador deve voltar para a posição/dimensão original, sem ficar preso no Vértice Vazio.
- [ ] Mate somente o outro participante: ele sai da Zona ao renascer, mas a sessão do conjurador continua normalmente.
- [ ] Faça William cair de uma parte elevada da espiral: sem dano de queda. Faça outro participante cair: ele não recebe essa imunidade (o resgate de void continua ativo conforme a regra anterior).
- [ ] Teste fogo/lava enquanto a Reescrita Dracônica está ativa: William deve estar protegido. Após a ultimate terminar e o efeito curto expirar, volta às condições normais.
- [ ] Confira que o cooldown de 500 s é aplicado também quando **outro jogador** entra no quadrado central e encerra a Zona.

Se precisar repetir rapidamente os testes, use `/minefables_william reset_cooldowns` como OP; o comando remove os cooldowns e não faz parte do gameplay.

## Validação

Checagem de sintaxe Java 21 nos três arquivos, compatibilidade de assinaturas principais no JAR NeoForge 21.1.265 e integridade do ZIP concluídas. **Não houve compilação Gradle completa nem teste dentro do Minecraft neste ambiente.**
