# MineFables — William: VFX + Bola de Fogo + queda no Vértice Vazio (v8)

## Compatibilidade

Este é um patch incremental sobre o rework do William **já atualizado até a Zona Fabular v7**. Preserva a HUD/roda de habilidades, Fulgor GeckoLib e o comando OP de reset de cooldown.

Minecraft 1.21.1, NeoForge 21.1.x, Java 21. Não acrescenta bibliotecas nem scripts Python.

## Instalação

1. Salve/commite ou faça backup do projeto local.
2. Extraia o ZIP na raiz do repositório, mesclando `src` e sobrescrevendo os 4 arquivos existentes; o quinto (`WilliamVfx.java`) é novo.
3. `gradlew.bat compileJava` e `gradlew.bat runClient`.
4. Use um mundo de testes e cheque a compilação antes de comitar.

## Principais correções

- **Bola de Fogo**: a aceleração antes era zero e a velocidade caía 5% por tick, aproximando-se da imobilidade. Agora há propulsão 0,06 + velocidade inicial de 1,05 bloco/tick, rastro visível e descarte após 70 ticks sem colisão. Não cria fogo nos blocos. O dano por impacto continua o original.
- **Vértice Vazio**: resgate fora do centro somente em Y<18, de modo que o jogador atravesse a camada Y=20. William pode se reposicionar ao cair pelo quadrado central X/Z [-5,+5] quando chegar a Y<=20; demais jogadores ainda acionam saída pelo quadrado em qualquer altura, conforme a regra anterior.
- **Proteção**: a imunidade de William passa a ser apenas ao dano do void (não a todo tipo de dano).
- **Títulos e mensagens**: removidos dos castings, assim como mensagens de sucesso/erro repetitivas via actionbar. A HUD continua indicando cooldowns e temperatura; mensagens OP/instalação permanecem.
- **Efeitos sem Photon**: partículas e sons específicos para Ressonância (potências e aura), Impétuo (ecos no alvo), Caminhante da Luz (vínculo/transporte/abertura), Citação (invocação/desaparecimento), Mergulhar (marca, canalização, viagem), Reescrita Dracônica (ativação e calor), Bola de Fogo (rastro/impacto), Impulso de Vapor (trilha de deslocamento), Sobrecarga (espirais e explosão), Zona Fabular (portal e reaparecimento). O anel do Impétuo na mão não foi alterado.

## Testes sugeridos

1. No nível 6, veja cada habilidade em terceira pessoa e em multiplayer; confira a ausência de textos grandes sobre a tela.
2. Ressonância: potências I, II, III com visual crescente; desative para ver a dissipação.
3. Impétuo: prepare, acerte e aguarde ecos retardados; com Fulgor confirme o terceiro golpe e partículas elétricas.
4. Citação: 15 segundos de arma, efeito ao sumir; o cooldown só começa nesse momento.
5. Mergulhar: marque com Shift+T; canalize durante 5 segundos; teste cancelamento por dano e depois salto normal.
6. Bola de Fogo: atire para o céu sem alvo; confira que avança durante o voo, deixa rastro e desaparece sozinha em 70 ticks (~3,5s). Atire num bloco e veja que não cria fogo; atire em um mob para confirmar o dano.
7. Impulso de Vapor e Sobrecarga: observe as trilhas e o vórtice de 15 blocos.
8. Vértice Vazio: com William, caia fora do quadrado central X/Z [-5,+5] e confirme que atravessa Y=20 antes do resgate (Y<18). Depois caia PELO CENTRO e confirme a mecânica de reaparecimento em Y<=20. Um segundo jogador que entra no quadrado central ainda encerra a Zona.
9. Teste `/minefables_william reset_cooldowns` como OP após usar as habilidades.

## Não alterado e limitações

A Zona continua usando o NBT já instalado e dura 60 segundos; o clash ainda não foi implementado. A bola de fogo continua com dano de fogo vanilla até a integração específica com Iron's Spells. Os VFX são partículas nativas, não shaders/bloom ou modelos 3D novos. O pacote foi analisado estruturalmente e as APIs principais foram conferidas no JAR 21.1.265; **não foi possível executar o Gradle completo nem testar dentro do Minecraft neste ambiente**.
