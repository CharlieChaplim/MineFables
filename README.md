
# MineFables

Expansive Minecraft mod focused on playable characters,
factions, progression, abilities and custom gameplay systems.

## Technical Stack

- Minecraft: 1.21.1
- Mod Loader: NeoForge
- Java: 21
- Build System: Gradle

## Development

### Run the development client

```powershell
.\gradlew.bat runClient
```

### Build the mod

```powershell
.\gradlew.bat build
```

### Clean build

```powershell
.\gradlew.bat clean build
```

## Project Status

Early development. William and ability/resource systems are under active development.

## William — Dragon Install

Ao ativar **Dragon Override**, o servidor envia um pacote de estado visual para jogadores
proximos. Durante a ultimate, a armadura vanilla equipada deixa de ser desenhada e a
armadura personalizada aparece sobre o modelo do jogador (terceira pessoa). A
armadura real no inventario e seus atributos nao sao modificados pela camada visual.
A camada desaparece ao encerrar a ultimate.

O projeto depende de **GeckoLib 4.9.3** (NeoForge 1.21.1); o Gradle baixa a
biblioteca do Maven oficial. Para executar o JAR publicado fora do ambiente de
desenvolvimento, instale tambem o mod GeckoLib correspondente em `mods/`.

O desenho da armadura utiliza um mesh estatico integrado ao `PlayerRenderer`;
GeckoLib esta pronto para ser utilizado em animacoes posteriores, mas este modelo
ainda nao possui animacoes GeckoLib. Os grupos do modelo enviado possuem geometria
na cabeca e no braco direito; os demais ainda nao foram modelados.

Arquivos de arte:
- `art/source/william/dragon_install/CustomArmor.bbmodel`: projeto editavel Blockbench.
- `art/source/william/dragon_install/texture.png`: textura editavel usada para exportacao.
- `src/main/resources/assets/minefables/textures/entity/william/dragon_install.png`: textura do jogo.
- `src/main/resources/assets/minefables/mesh/william/dragon_install_mesh.json`: geometria
  utilizada pela camada do jogador (com UVs extraidas do modelo fornecido).
- `src/main/resources/assets/minefables/geo/william/dragon_install.geo.json`: exportacao
  GeckoLib reservada para futuras animacoes.

### Testar a transformacao

1. Use um mundo de testes com cheats; selecione William (`/minefables character set william`).
2. Obtenha quatro paginas (`/give @p minefables:william_page 4`) e use-as para atingir nivel 5.
3. Equipe armadura vanilla, ative Dragon Override e olhe em terceira pessoa (`F5`).
4. A armadura vanilla deve desaparecer VISUALMENTE, sem ser removida do inventario;
   as partes prontas do modelo Dragon Install devem aparecer no jogador.
5. Espere o fim da ultimate: a armadura vanilla deve aparecer novamente.
6. Repita com outro jogador na sessao para verificar sincronizacao do estado visual.

**Limite atual:** a visualizacao em primeira pessoa (bracos) nao foi substituida.

