package com.github.liudaomanbu.excel.config;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Sheet;
import com.github.liudaomanbu.excel.constant.Direction;
import com.github.liudaomanbu.excel.parse.result.Table;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;

public class TableConfig<T> extends Config<Table<T>> {
  public static class Builder<T> extends Config.Builder<Table<T>> {
    private Collection<MenuConfig.Builder<T>> topMenuConfigBuilders;
    private SheetConfig sheetConfig;
    private Direction menuDirection;
    private TableDataConfig.Builder<T> dataConfigBuilder;

    public Builder() {
      topMenuConfigBuilders = Lists.newLinkedList();
    }

    public TableConfig<T> build() {
      return new TableConfig<>(this);
    }

    @Override
    public Builder<T> setId(Object id) {
      super.setId(id);
      return this;
    }

    public SheetConfig getSheetConfig() {
      return sheetConfig;
    }

    public Builder<T> setSheetConfig(SheetConfig sheetConfig) {
      this.sheetConfig = sheetConfig;
      return this;
    }

    public Direction getMenuDirection() {
      return menuDirection;
    }

    public Builder<T> setMenuDirection(Direction menuDirection) {
      this.menuDirection = menuDirection;
      return this;
    }

    public Collection<MenuConfig.Builder<T>> getTopMenuConfigBuilders() {
      return topMenuConfigBuilders;
    }

    public Builder<T> setTopMenuConfigBuilders(
        Collection<MenuConfig.Builder<T>> topMenuConfigBuilders) {
      this.topMenuConfigBuilders = topMenuConfigBuilders;
      return this;
    }

    public TableDataConfig.Builder<T> getDataConfigBuilder() {
      return dataConfigBuilder;
    }

    public Builder<T> setDataConfigBuilder(TableDataConfig.Builder<T> dataConfigBuilder) {
      this.dataConfigBuilder = dataConfigBuilder;
      return this;
    }

  }

  public static final Direction DEFAULT_MENU_DIRECTION = Direction.BOTTOM;

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  private final SheetConfig sheetConfig;
  private final Direction menuDirection;
  private final ImmutableCollection<MenuConfig<T>> topMenuConfigs;
  private final TableDataConfig<T> dataConfig;

  private final Traverser<MenuConfig<T>> MENU_CONFIG_TRAVERSER =
      Traverser.forTree(new SuccessorsFunction<MenuConfig<T>>() {
        @Override
        public Iterable<? extends MenuConfig<T>> successors(MenuConfig<T> node) {
          return node.getChildrens();
        }
      });

  private TableConfig(Builder<T> builder) {
    super(builder);
    sheetConfig = builder.sheetConfig;
    menuDirection = Optional.ofNullable(builder.menuDirection).orElse(DEFAULT_MENU_DIRECTION);
    topMenuConfigs = builder.topMenuConfigBuilders.stream()
        .peek(topMenuConfigBuilder -> topMenuConfigBuilder.setTableConfig(this))
        .map(MenuConfig.Builder::build).collect(ImmutableSet.toImmutableSet());
    dataConfig = builder.dataConfigBuilder.setTableConfig(this).build();
  }

  public Table.Builder<T> parse(Sheet sheet) {
    Table.Builder<T> builder = Table.<T>builder().setConfig(this);
    return builder;
  }

  public ParserConfig getEffectiveParserConfig() {
    return Optional.ofNullable(getParserConfig()).orElseGet(sheetConfig::getEffectiveParserConfig);
  }

  public Stream<MenuConfig<T>> getMenuConfigs() {
    return topMenuConfigs.stream().map(MENU_CONFIG_TRAVERSER::breadthFirst)
        .flatMap(Streams::stream);
  }

  public SheetConfig getSheetConfig() {
    return sheetConfig;
  }

  public ImmutableCollection<MenuConfig<T>> getTopMenuConfigs() {
    return topMenuConfigs;
  }


  public Direction getMenuDirection() {
    return menuDirection;
  }

  public TableDataConfig<T> getDataConfig() {
    return dataConfig;
  }

}
