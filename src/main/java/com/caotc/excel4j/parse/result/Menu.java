package com.caotc.excel4j.parse.result;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.caotc.excel4j.config.MenuConfig;
import com.caotc.excel4j.constant.Direction;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class Menu<V> {
  public static class Builder<V> {
    private StandardCell cell;
    private MenuConfig<V> menuConfig;
    private Table table;
    private Menu<?> parentMenu;

    public Menu<V> build() {
      Preconditions.checkNotNull(cell);
      Preconditions.checkArgument(table != null || parentMenu != null);
      Preconditions.checkArgument(menuConfig != null || parentMenu != null);

      if (parentMenu != null && table == null) {
        table = parentMenu.table;
      }
      return new Menu<V>(this);
    }

    public StandardCell getCell() {
      return cell;
    }

    public Builder<V> setCell(StandardCell cell) {
      this.cell = cell;
      return this;
    }

    public MenuConfig<V> getMenuConfig() {
      return menuConfig;
    }

    public Builder<V> setMenuConfig(MenuConfig<V> menuConfig) {
      this.menuConfig = menuConfig;
      return this;
    }

    public Table getTable() {
      return table;
    }

    public Builder<V> setTable(Table table) {
      this.table = table;
      return this;
    }

    public Menu<?> getParentMenu() {
      return parentMenu;
    }

    public Builder<V> setParentMenu(Menu<?> parentMenu) {
      this.parentMenu = parentMenu;
      return this;
    }

  }

  public static <V> Builder<V> builder() {
    return new Builder<V>();
  }

  private final StandardCell cell;
  private final MenuConfig<V> menuConfig;
  private final Table table;
  private final Menu<?> parentMenu;
  private final ImmutableList<Menu<?>> childrenMenus;
  private final Data<V> data;

  public Menu(Builder<V> builder) {
    cell = builder.cell;
    menuConfig = builder.menuConfig;
    table = builder.table;
    parentMenu = builder.parentMenu;

    childrenMenus = loadChildrenMenus();
    data = new Data<V>(this,childrenMenus.isEmpty() ? menuConfig.getDataConfig().getLoadType().getDataCells(this)
        : ImmutableList.of());
  }

  private <T> ImmutableList<Menu<?>> loadChildrenMenus() {
    ImmutableCollection<MenuConfig<?>> childrenConfigs = menuConfig.getChildrenMenuConfigs();

    List<StandardCell> menuCells =
        menuConfig.getDirection().get(getCell(), menuConfig.getDistance());
    FluentIterable<Menu<?>> menus= FluentIterable.from(menuCells).transform(cell -> {
      Iterable<MenuConfig<?>> configs =
          Iterables.filter(childrenConfigs, config -> config.matches(cell));
      Preconditions.checkState(Iterables.size(configs) <= 1);
      Menu<?> menu = null;
      if (!Iterables.isEmpty(configs)) {
        Builder builder = Menu.builder();
        builder.setCell(cell);
        builder.setMenuConfig(Iterables.getOnlyElement(configs));
        builder.setParentMenu(this);
        menu = builder.build();
      }
      return Optional.ofNullable(menu);
    }).filter(Optional::isPresent).transform(Optional::get);
    return menus.toList();
  }

  public void checkDataCell(StandardCell dataCell) {
    // if (menuConfig.getDataMatcher() != null) {
    // TODO
    // Object value = dataCell.getValue();
    // if(!checkMenuConfig.getDataMatcher().matches(value)){
    // StringBuffer errorMessage=new StringBuffer();
    // errorMessage.append("工作簿").append(dataCell.getSheet().getSheetName()).append("第").append(dataCell
    // .getRowIndex()+1).append("行").append(ExcelUtil.indexToColumn(dataCell.getColumnIndex()+1))
    // .append("列的").append(getName()).append("数据格式不正确");
    // addError(errorMessage.toString());
    // }

    // if(StringUtils.isEmpty(cellString)){
    // if(checkMenuConfig.isCheckEmpty()){
    // errorMessage.append("不能为空");
    // addError(errorMessage.toString());
    // }
    // }else{
    // Integer maxLength=checkMenuConfig.getMaxLength();
    // if(maxLength!=null && maxLength>=0 && cellString.length()>maxLength){
    // errorMessage.append("长度超过").append(checkMenuConfig.getMaxLength());
    // addError(errorMessage.toString());
    // }
    //
    // String regex=checkMenuConfig.getRegex();
    // if(regex!=null){
    // regex+=checkMenuConfig.isCheckEmpty()?"+":"*";
    // if(!cellString.matches(regex)){
    // String regexTip=ExcelUtil.REGEX_AND_TIP_MAP.get(checkMenuConfig.getRegex());
    // if(regexTip!=null){
    // errorMessage.append("数据格式不正确，应为").append(regexTip);
    // }else{
    // errorMessage.append("数据格式不正确");
    // }
    // addError(errorMessage.toString());
    // }
    // }
    // }
    // }
  }

  public Optional<Menu<?>> getSuper(Predicate<? super Menu<?>> predicate) {
    Preconditions.checkNotNull(predicate);
    if (isTopMenu()) {
      return Optional.empty();
    }
    return predicate.apply(parentMenu) ? Optional.of(parentMenu) : parentMenu.getSuper(predicate);
  }

  public Optional<Menu<?>> getFieldParent() {
    return getSuper(menu -> menu.getFieldName().isPresent());
  }

  public FluentIterable<Menu<?>> getSubs(Predicate<? super Menu<?>> predicate) {
    Preconditions.checkNotNull(predicate);
    if (isDataMenu()) {
      return FluentIterable.of();
    }
    FluentIterable<Menu<?>> subs = FluentIterable.from(childrenMenus).filter(predicate);
    return subs.isEmpty()
        ? FluentIterable.from(childrenMenus).transformAndConcat(menu -> menu.getSubs(predicate))
        : subs;
  }

  public FluentIterable<Menu<?>> getFieldChildrens() {
    return getSubs(menu -> menu.getFieldName().isPresent());
  }

  public ImmutableList<Field> getFields() {
    if (!getField().isPresent()) {
      return ImmutableList.of();
    }
    com.google.common.collect.ImmutableList.Builder<Field> builder = ImmutableList.builder();
    builder.add(getField().get());
    Optional<Menu<?>> optional = getFieldParent();
    while (optional.isPresent()) {
      Menu<?> menu = optional.get();
      builder.add(menu.getField().get());
      optional = menu.getFieldParent();
    }
    return builder.build().reverse();
  }

  public Optional<StandardCell> nextDataCell(StandardCell cell) {
    if (cell == null) {
      cell = this.cell;
    }

    Direction direction = menuConfig.getDirection();

    return this.cell.equals(cell) ? direction.getCell(cell, menuConfig.getDistance())
        : direction.nextCell(cell);
  }

  public boolean hasChildrenMenu(Menu<?> childrenMenu) {
    return childrenMenus.contains(childrenMenu);
  }

  public boolean hasChildrenMenu(StandardCell cell) {
    return Iterables.any(childrenMenus, childrenMenu -> childrenMenu.getCell().equals(cell));
  }

  public String getName() {
    return cell.getValueCell().getStringCellValue();
  }

  public Optional<Field> getField() {
    return menuConfig.getField();
  }

  public Optional<String> getFieldName() {
    return menuConfig.getFieldName();
  }

  // delegate methods start
  public boolean isTopMenu() {
    return menuConfig.isTopMenu();
  }

  public boolean isDataMenu() {
    return menuConfig.isDataMenu();
  }

  public boolean isFixedDataMenu() {
    return menuConfig.isFixedDataMenu();
  }

  public boolean isUnFixedDataMenu() {
    return menuConfig.isUnFixedDataMenu();
  }

  public boolean isMixedDataMenu() {
    return menuConfig.isMixedDataMenu();
  }

  public boolean isMustMenu() {
    return menuConfig.isMustMenu();
  }

  public boolean isNotMustMenu() {
    return menuConfig.isNotMustMenu();
  }

  public boolean matches(Object value) {
    return menuConfig.matches(value);
  }

  public boolean support(Object value) {
    return menuConfig.support(value);
  }

  public Collection<Class<?>> canCastClasses() {
    return menuConfig.canCastClasses();
  }

  public <T> boolean canCast(Class<T> clazz) {
    return menuConfig.canCast(clazz);
  }

  public <T> T cast(Object value, Class<T> clazz) {
    return menuConfig.cast(value, clazz);
  }
  // delegate methods end

  public StandardCell getCell() {
    return cell;
  }

  public MenuConfig<V> getMenuConfig() {
    return menuConfig;
  }

  public Menu<?> getParentMenu() {
    return parentMenu;
  }

  public Table getTable() {
    return table;
  }

  public ImmutableList<Menu<?>> getChildrenMenus() {
    return childrenMenus;
  }

  public Data<V> getData() {
    return data;
  }

}
