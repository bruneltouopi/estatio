/*
 * Copyright 2012-2015 Eurocommercial Properties NV
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.estatio.module.application.imports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Nature;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.Publishing;
import org.apache.isis.applib.annotation.RenderType;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.ViewModelLayout;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.services.message.MessageService;
import org.apache.isis.applib.services.registry.ServiceRegistry2;
import org.apache.isis.applib.services.wrapper.WrapperFactory;
import org.apache.isis.applib.value.Blob;

import org.isisaddons.module.excel.dom.ExcelService;
import org.isisaddons.module.excel.dom.WorksheetContent;
import org.isisaddons.module.excel.dom.WorksheetSpec;

import org.estatio.module.asset.dom.Property;
import org.estatio.module.asset.dom.PropertyRepository;
import org.estatio.module.budget.dom.budget.Budget;
import org.estatio.module.budget.dom.budget.BudgetRepository;
import org.estatio.module.budget.dom.keyitem.DirectCost;
import org.estatio.module.budget.dom.keyitem.KeyItem;
import org.estatio.module.budget.dom.keytable.DirectCostTable;
import org.estatio.module.budget.dom.keytable.KeyTable;
import org.estatio.module.budget.dom.keytable.PartitioningTableRepository;
import org.estatio.module.budgetassignment.contributions.Budget_Remove;
import org.estatio.module.budgetassignment.imports.DirectCostLine;
import org.estatio.module.budgetassignment.imports.KeyItemImportExportLineItem;
import org.estatio.module.budgetassignment.imports.KeyItemImportExportService;
import org.estatio.module.budgetassignment.imports.Status;
import org.estatio.module.charge.imports.ChargeImport;

import lombok.Getter;
import lombok.Setter;

// TODO: need to untangle this and push back down to budget module
@DomainObject(
        nature = Nature.VIEW_MODEL,
        objectType = "org.estatio.app.services.budget.BudgetImportExportManager"
)
@DomainObjectLayout(
        named = "Import Export manager for budget",
        bookmarking = BookmarkPolicy.AS_ROOT
)
@ViewModelLayout()
public class BudgetImportExportManager {

    public String title() {
        return "Import / Export manager for budget";
    }

    public BudgetImportExportManager() {
        this.name = "Budget Import / Export";
        this.fileName = "export.xlsx";
    }

    public BudgetImportExportManager(Budget budget) {
        this();
        this.budget = budget;
    }

    public BudgetImportExportManager(BudgetImportExportManager budgetImportExportManager) {
        this.fileName = budgetImportExportManager.getFileName();
        this.name = budgetImportExportManager.getName();
        this.budget = budgetImportExportManager.getBudget();
    }

    @Getter @Setter
    private String name;
    @Getter @Setter
    private Budget budget;
    @Getter @Setter
    private String fileName;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    @ActionLayout(named = "Change File Name")
    @MemberOrder(name = "fileName", sequence = "1")
    public BudgetImportExportManager changeFileName(final String fileName) {
        this.setFileName(fileName);
        return new BudgetImportExportManager(this);
    }

    public String default0ChangeFileName() {
        return getFileName();
    }

    @SuppressWarnings("unchecked")
    @Collection()
    @CollectionLayout(
            render = RenderType.EAGERLY
    )
    public List<BudgetImportExport> getLines() {
        return budgetImportExportService.lines(this);
    }

    public List<KeyItemImportExportLineItem> getKeyItemLines() {
        List<KeyItemImportExportLineItem> result = new ArrayList<>();
        if (getBudget()==null){return result;} // for import from menu where budget unknown
        for (KeyTable keyTable : this.getBudget().getKeyTables()){
            result.addAll(keyItemImportExportService.items(keyTable.getItems()));
        }
        return result;
    }

    public List<DirectCostLine> getDirectCostLines() {
        List<DirectCostLine> result = new ArrayList<>();
        if (getBudget()==null){return result;} // for import from menu where budget unknown
        for (DirectCostTable directCostTable : this.getBudget().getDirectCostTables()){
            result.addAll(keyItemImportExportService.directCosts(directCostTable.getItems()));
        }
        return result;
    }

    public List<ChargeImport> getCharges() {
        List<ChargeImport> result = new ArrayList<>();
        if (getBudget()==null){return result;} // for import from menu where budget unknown
        return budgetImportExportService.charges(this);
    }


    @Action(publishing = Publishing.DISABLED, semantics = SemanticsOf.IDEMPOTENT)
    @ActionLayout()
    @CollectionLayout()
    public Budget importBudget(
            @Parameter(fileAccept = ".xlsx")
            @ParameterLayout(named = "Excel spreadsheet")
            final Blob spreadsheet) {

        WorksheetSpec spec1 = new WorksheetSpec(BudgetImportExport.class, "budget");
        WorksheetSpec spec2 = new WorksheetSpec(KeyItemImportExportLineItem.class, "keyItems");
        WorksheetSpec spec3 = new WorksheetSpec(DirectCostLine.class, "directCosts");
        WorksheetSpec spec4 = new WorksheetSpec(ChargeImport.class, "charges");
        List<List<?>> objects =
                excelService.fromExcel(spreadsheet, Arrays.asList(spec1, spec2, spec3, spec4));

        // try to remove budget if exists
        List<BudgetImportExport> lineItems = (List<BudgetImportExport>) objects.get(0);
        Budget budgetToRemove = getBudgetUsingFirstLine(lineItems);
        String errorMessage = null;
        try {
            if (this.getBudget()!=null) {
                wrapperFactory.wrap(factoryService.mixin(Budget_Remove.class, budgetToRemove)).removeBudget(true);
            }
        } catch (Exception e) {
            errorMessage = "Import cannot be performed. Error: " + e.getMessage();
            messageService.warnUser(errorMessage);
        }

        if (errorMessage==null) {
            // first upsert charges
            List<ChargeImport> chargeImportLines = (List<ChargeImport>) objects.get(3);
            for (ChargeImport lineItem : chargeImportLines) {
                lineItem.importData(null);
            }

            // import budget and items
            List<BudgetImportExport> budgetItemLines = importBudgetAndItems(objects);

            // import keyTables
            importKeyTables(budgetItemLines, objects);

            // import directCosts
            importDirectCostTables(budgetItemLines, objects);
        }

        return getBudget();
    }

    private Budget getBudgetUsingFirstLine(List<BudgetImportExport> lineItems){
        BudgetImportExport firstLine = lineItems.get(0);
        Property property = propertyRepository.findPropertyByReference(firstLine.getPropertyReference());
        return budgetRepository.findByPropertyAndStartDate(property, firstLine.getBudgetStartDate());
    }

    private List<BudgetImportExport> importBudgetAndItems(final List<List<?>> objects){
        Budget importedBudget = new Budget();
        List<BudgetImportExport> lineItems = (List<BudgetImportExport>) objects.get(0);
        BudgetImportExport previousRow = null;
        for (BudgetImportExport lineItem :lineItems){
            importedBudget = (Budget) lineItem.importData(previousRow).get(0);
            previousRow = lineItem;
        }
        setBudget(importedBudget);
        return lineItems;
    }

    private void importKeyTables(final List<BudgetImportExport> budgetItemLines, final List<List<?>> objects){

        List<KeyTable> keyTablesToImport = keyTablesToImport(budgetItemLines);
        List<KeyItemImportExportLineItem> keyItemLines = (List<KeyItemImportExportLineItem>) objects.get(1);

        // filter case where no key items are filled in
        if (keyItemLines.size() == 0) {return;}

        for (KeyTable keyTable : keyTablesToImport){
            List<KeyItemImportExportLineItem> itemsToImportForKeyTable = new ArrayList<>();
            for (KeyItemImportExportLineItem keyItemLine : keyItemLines){
                if (keyItemLine.getKeyTableName().equals(keyTable.getName())){
                    itemsToImportForKeyTable.add(new KeyItemImportExportLineItem(keyItemLine));
                }
            }
            for (KeyItem keyItem : keyTable.getItems()) {
                Boolean keyItemFound = false;
                for (KeyItemImportExportLineItem lineItem : itemsToImportForKeyTable){
                    if (lineItem.getUnitReference().equals(keyItem.getUnit().getReference())){
                        keyItemFound = true;
                        break;
                    }
                }
                if (!keyItemFound) {
                    KeyItemImportExportLineItem deletedItem = new KeyItemImportExportLineItem(keyItem, null);
                    deletedItem.setStatus(Status.DELETED);
                    itemsToImportForKeyTable.add(deletedItem);
                }
            }
            for (KeyItemImportExportLineItem item : itemsToImportForKeyTable){
                serviceRegistry2.injectServicesInto(item);
                item.validate();
                item.apply();
            }
        }
    }

    private List<KeyTable> keyTablesToImport(final List<BudgetImportExport> lineItems){
        List<KeyTable> result = new ArrayList<>();
        for (BudgetImportExport lineItem :lineItems) {
            if (PartitioningTableType.valueOf(lineItem.getTableType()) == PartitioningTableType.KEY_TABLE) {
                KeyTable foundKeyTable = (KeyTable) partitioningTableRepository.findByBudgetAndName(getBudget(), lineItem.getPartitioningTableName());
                if (foundKeyTable != null && !result.contains(foundKeyTable)) {
                    result.add(foundKeyTable);
                }
            }
        }
        return result;
    }

    private void importDirectCostTables(final List<BudgetImportExport> budgetItemLines, final List<List<?>> objects){

        List<DirectCostTable> tablesToImport = directCostTablesToImport(budgetItemLines);
        List<DirectCostLine> lines = (List<DirectCostLine>) objects.get(2);

        // filter case where no key items are filled in
        if (lines.size() == 0) {return;}

        for (DirectCostTable table : tablesToImport){
            List<DirectCostLine> itemsToImportForTable = new ArrayList<>();
            for (DirectCostLine line : lines){
                if (line.getDirectCostTableName().equals(table.getName())){
                    itemsToImportForTable.add(new DirectCostLine(line));
                }
            }
            for (DirectCost directCost : table.getItems()) {
                Boolean itemFound = false;
                for (DirectCostLine lineItem : itemsToImportForTable){
                    if (lineItem.getUnitReference().equals(directCost.getUnit().getReference())){
                        itemFound = true;
                        break;
                    }
                }
                if (!itemFound) {
                    DirectCostLine deletedItem = new DirectCostLine(directCost, null);
                    deletedItem.setStatus(Status.DELETED);
                    itemsToImportForTable.add(deletedItem);
                }
            }
            for (DirectCostLine item : itemsToImportForTable){
                serviceRegistry2.injectServicesInto(item);
                item.validate();
                item.apply();
            }
        }
    }

    private List<DirectCostTable> directCostTablesToImport(final List<BudgetImportExport> lineItems){
        List<DirectCostTable> result = new ArrayList<>();
        for (BudgetImportExport lineItem :lineItems) {
            if (PartitioningTableType.valueOf(lineItem.getTableType()) == PartitioningTableType.DIRECT_COST_TABLE) {
                DirectCostTable foundTable = (DirectCostTable) partitioningTableRepository.findByBudgetAndName(getBudget(), lineItem.getPartitioningTableName());
                if (foundTable != null && !result.contains(foundTable)) {
                    result.add(foundTable);
                }
            }
        }
        return result;
    }

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-download")
    public Blob exportBudget() {
        final String fileName = withExtension(getFileName(), ".xlsx");
        WorksheetSpec spec1 = new WorksheetSpec(BudgetImportExport.class, "budget");
        WorksheetSpec spec2 = new WorksheetSpec(KeyItemImportExportLineItem.class, "keyItems");
        WorksheetSpec spec3 = new WorksheetSpec(DirectCostLine.class, "directCosts");
        WorksheetSpec spec4 = new WorksheetSpec(ChargeImport.class, "charges");
        WorksheetContent worksheetContent = new WorksheetContent(getLines(), spec1);
        WorksheetContent keyItemsContent = new WorksheetContent(getKeyItemLines(), spec2);
        WorksheetContent directCostsContent = new WorksheetContent(getDirectCostLines(), spec3);
        WorksheetContent chargesContent = new WorksheetContent(getCharges(), spec4);
        return excelService.toExcel(Arrays.asList(worksheetContent, keyItemsContent, directCostsContent, chargesContent), fileName);

    }

    public String disableExportBudget() {
        return getFileName() == null ? "file name is required" : null;
    }

    private static String withExtension(final String fileName, final String fileExtension) {
        return fileName.endsWith(fileExtension) ? fileName : fileName + fileExtension;
    }
    
    @Inject
    private ExcelService excelService;

    @Inject
    private BudgetImportExportService budgetImportExportService;

    @Inject
    private KeyItemImportExportService keyItemImportExportService;
    
    @Inject
    PartitioningTableRepository partitioningTableRepository;

    @Inject
    private ServiceRegistry2 serviceRegistry2;

    @Inject
    private BudgetRepository budgetRepository;

    @Inject
    PropertyRepository propertyRepository;

    @Inject FactoryService factoryService;

    @Inject WrapperFactory wrapperFactory;

    @Inject MessageService messageService;

}
