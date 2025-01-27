package io.anuke.mindustry.world.blocks.units;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.Effects.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.units.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class CommandCenter extends Block{
    protected TextureRegion[] commandRegions = new TextureRegion[UnitCommand.all.length];
    protected Color topColor = Pal.command;
    protected Color bottomColor = Color.valueOf("5e5e5e");
    protected Effect effect = Fx.commandSend;

    public CommandCenter(String name){
        super(name);

        flags = EnumSet.of(BlockFlag.comandCenter);
        destructible = true;
        solid = true;
        configurable = true;
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);
        ObjectSet<Tile> set = indexer.getAllied(tile.getTeam(), BlockFlag.comandCenter);

        if(set.size > 0){
            CommandCenterEntity entity = tile.entity();
            CommandCenterEntity oe = set.first().entity();
            entity.command = oe.command;
        }
    }

    @Override
    public void load(){
        super.load();

        for(UnitCommand cmd : UnitCommand.all){
            commandRegions[cmd.ordinal()] = Core.atlas.find("icon-command-" + cmd.name() + "-small");
        }
    }

    @Override
    public void draw(Tile tile){
        CommandCenterEntity entity = tile.entity();
        super.draw(tile);

        float size = iconsizesmall/4f;

        Draw.color(bottomColor);
        Draw.rect(commandRegions[entity.command.ordinal()], tile.drawx(), tile.drawy() - 1, size, size);
        Draw.color(topColor);
        Draw.rect(commandRegions[entity.command.ordinal()], tile.drawx(), tile.drawy(), size, size);
        Draw.color();
    }

    @Override
    public void buildTable(Tile tile, Table table){
        CommandCenterEntity entity = tile.entity();
        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        Table buttons = new Table();

        for(UnitCommand cmd : UnitCommand.all){
            buttons.addImageButton("icon-command-" + cmd.name() + "-small", "clear-toggle-trans", iconsizesmall, () -> Call.onCommandCenterSet(player, tile, cmd))
            .size(44).group(group).update(b -> b.setChecked(entity.command == cmd));
        }
        table.add(buttons);
        table.row();
        table.label(() -> entity.command.localized()).style("outline").center().growX().get().setAlignment(Align.center);
    }

    @Remote(called = Loc.server, forward = true, targets = Loc.both)
    public static void onCommandCenterSet(Player player, Tile tile, UnitCommand command){
        Effects.effect(((CommandCenter)tile.block()).effect, tile);

        for(Tile center : indexer.getAllied(tile.getTeam(), BlockFlag.comandCenter)){
            if(center.block() instanceof CommandCenter){
                CommandCenterEntity entity = center.entity();
                entity.command = command;
            }
        }

        Team team = (player == null ? tile.getTeam() : player.getTeam());

        for(BaseUnit unit : unitGroups[team.ordinal()].all()){
            unit.onCommand(command);
        }
    }

    @Override
    public TileEntity newEntity(){
        return new CommandCenterEntity();
    }

    public class CommandCenterEntity extends TileEntity{
        public UnitCommand command = UnitCommand.attack;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeByte(command.ordinal());
        }

        @Override
        public void read(DataInput stream, byte version) throws IOException{
            super.read(stream, version);
            command = UnitCommand.all[stream.readByte()];
        }
    }
}
