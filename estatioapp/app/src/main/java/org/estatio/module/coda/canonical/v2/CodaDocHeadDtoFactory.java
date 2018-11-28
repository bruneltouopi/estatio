package org.estatio.module.coda.canonical.v2;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;

import org.estatio.canonical.coda.v2.CodaDocHeadDto;
import org.estatio.canonical.common.v2.CodaDocKey;
import org.estatio.module.base.platform.applib.DtoFactoryAbstract;
import org.estatio.module.coda.dom.doc.CodaDocHead;

@DomainService(
        nature = NatureOfService.DOMAIN,
        objectType = "coda.canonical.v2.CodaDocHeadDtoFactory"
)
public class CodaDocHeadDtoFactory extends DtoFactoryAbstract<CodaDocHead, CodaDocHeadDto> {

    public CodaDocHeadDtoFactory() {
        super(CodaDocHead.class, CodaDocHeadDto.class);
    }

    protected CodaDocHeadDto newDto(final CodaDocHead codaDocHead) {
        final CodaDocHeadDto dto = new CodaDocHeadDto();

        dto.setSelf(mappingHelper.oidDtoFor(codaDocHead));

        final CodaDocKey docKey = new CodaDocKey();
        docKey.setCmpCode(codaDocHead.getCmpCode());
        docKey.setDocCode(codaDocHead.getDocCode());
        docKey.setDocNum(codaDocHead.getDocNum());
        dto.setCodaDocKey(docKey);

        dto.setCodaPeriod(codaDocHead.getCodaPeriod());

        dto.setIncomingInvoice(mappingHelper.oidDtoFor(codaDocHead.getIncomingInvoice()));

        return dto;
    }

}
