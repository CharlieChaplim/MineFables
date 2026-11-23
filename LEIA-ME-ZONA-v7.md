# MineFables — Vértice Vazio: gatilho central por coordenadas (v7)

Instalação: copie a pasta `src` deste ZIP para a raiz do projeto, mesclando arquivos.

**Alteração:** a saída antecipada agora depende apenas de X/Z no centro da dimensão: `-5 <= X <= 5` e `-5 <= Z <= 5`, sem restrição de Y, velocidade de queda ou altura de dano do void. É um quadrado de 10×10 blocos (limites inclusivos).

- Outro jogador participante entra no quadrado: Zona termina e grupo retorna.
- William entra no quadrado: é reposicionado em superfície segura atrás de outro ser vivo participante; sem alvo, volta para superfície do lado original.
- Jogador fora do centro cai abaixo de Y=35: é resgatado na sua lateral para impedir que fique inalcançável.
- Não altera a estrutura NBT, raio de captura, nem os cooldowns ou duração.
- Não precisa reinstalar a arena.

Testes: compile com `.\gradlew.bat compileJava` e rode `.\gradlew.bat runClient`. Teste dois jogadores entrando no centro e a queda fora do quadrado.

**Limitação:** não houve build Gradle completo nem teste de multiplayer neste ambiente. O gatilho não tem intervalo vertical por solicitação; qualquer altura dentro do quadrado conta.
