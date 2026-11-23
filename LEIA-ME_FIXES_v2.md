# MineFables — hotfix William / UI / Vértice Vazio v2

Alvo: Minecraft 1.21.1 + NeoForge 21.1.x. **Incremental**: exige o rework William/Fulgor v1 aplicado no seu projeto.

## Correções

1. **Roda sem blur:** `AbilityWheelScreen` não chama mais `super.render` (que aplicava o fundo desfocado do menu). Mantém apenas uma camada escura sem desfoque.
2. **Citação:** cooldown de 15 segundos **começa quando a arma temporária desaparece**, inclusive quando a Citação é interrompida por troca de slot/menu/logout. Durante os 15 s de uso, não há cooldown correndo. A invocação não inicia mais o cooldown.
3. **Sobrecarga:** o fogo vanilla agora forma três braços espirais em um círculo com cerca de 15 blocos de diâmetro; partículas em vórtice animado por dois segundos, até ~7,5 blocos de raio.
4. **Vértice Vazio:** comando OP `/minefables_william instalar_zona` carrega os 256 chunks da arena **na dimensão `minefables:zona_fabular`** e posiciona o NBT embutido, centralizado. O comando **não** deve ser usado no meio de combate: a instalação pode causar uma pausa de alguns segundos; faça backup do mundo antes. Um bloco de bedrock técnico em `(0,-60,0)` sinaliza que foi instalado; executar novamente não sobrescreve a arena. O conjurador recebe aviso caso tente usar a Zona antes da instalação. O conserto de compilação `(Entity) null` também foi incorporado.
5. **Temperatura:** agora fica à direita da faixa de tempo da Reescrita Dracônica (ícone de temperatura + 4 segmentos + valor `0/4` a `4/4`).
6. **Impétuo:** pode ser preparado sem Ressonância. Custos, atrasos e cooldown existentes preservados.

## Aplicação

Extraia o ZIP na raiz do repositório, **mesclando** as pastas e substituindo os cinco arquivos Java correspondentes. Nenhum script precisa ficar no projeto. Faça `git status` antes de extrair para revisar modificações locais.

```powershell
.\gradlew.bat compileJava
.\gradlew.bat runClient
```

## Instalação da estrutura (uma única vez por mundo)

No mundo com cheats e usando um usuário OP, **depois de iniciar com o mod atualizado**, rode:

```mcfunction
/minefables_william instalar_zona
```

O comando posiciona o template diretamente na dimensão da Zona; não é necessário entrar nela nem usar `/place template` manual. **O template original de `228 × 76 × 240` deve estar presente** em `src/main/resources/data/minefables/structure/vertice_vazio.nbt` (já vem no rework v1). A estrutura inicia no Y=12, centralizada no X/Z zero, e os pads de entrada ficam no Y=94.

Se a mensagem disser que o template não foi localizado, confirme o recurso e reinicie o mundo. Se o comando não existir, verifique se aplicou o hotfix e compilou a nova versão. Caso a estrutura apareça na posição incorreta, **não** repita a instalação sem antes fazer backup e verificar a orientação do modelo: o comando está configurado sem espelhamento e sem rotação. A etapa de clash entre domínios ainda não está implementada.

## Testes rápidos

- Segurar `R`: cenário ao fundo **nítido**, apenas escurecido; soltar `R` seleciona. `T` continua ativando.
- Usar Citação: manter a arma por 15s, observar surgir cooldown **após** ela sumir. Também testar retirada antecipada por troca de slot.
- Ativar Sobrecarga num terreno aberto, afastado de construções, e confirmar aproximadamente 15 blocos de largura com fogo em espiral.
- Testar a HUD da ultimate: temperatura ao lado do cronômetro.
- Preparar Impétuo **sem Ressonância** e acertar um inimigo.
- OP: `/minefables_william instalar_zona`, depois lançar a Zona e verificar os pads e a arena. Verificar saída depois de 60s.

## Estado de validação

- Análise sintática de 48 arquivos Java: **sem erros de sintaxe**.
- Métodos Java para instalação da estrutura conferidos no JAR de desenvolvimento do NeoForge 1.21.1.
- **Gradle e teste no cliente não executados aqui:** o wrapper requer baixar Gradle 9.2.1, indisponível no ambiente. Execute a compilação acima no seu computador.
