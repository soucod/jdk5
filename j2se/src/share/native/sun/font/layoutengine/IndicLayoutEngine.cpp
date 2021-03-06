
/*
 * @(#)IndicLayoutEngine.cpp	1.9 03/10/24 
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LayoutEngine.h"
#include "OpenTypeLayoutEngine.h"
#include "IndicLayoutEngine.h"
#include "ScriptAndLanguageTags.h"

#include "GlyphSubstitutionTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"

#include "GDEFMarkFilter.h"

#include "IndicReordering.h"

IndicOpenTypeLayoutEngine::IndicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                    const GlyphSubstitutionTableHeader *gsubTable)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable), fMPreFixups(NULL)
{
    fFeatureOrder = IndicReordering::getFeatureOrder();
}

IndicOpenTypeLayoutEngine::IndicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode), fMPreFixups(NULL)
{
    fFeatureOrder = IndicReordering::getFeatureOrder();
}

IndicOpenTypeLayoutEngine::~IndicOpenTypeLayoutEngine()
{
    // nothing to do
}

// Input: characters, tags
// Output: glyphs, char indices
le_int32 IndicOpenTypeLayoutEngine::glyphProcessing(const LEUnicode chars[], 
    le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
    const LETag **&featureTags, LEGlyphID *&glyphs, le_int32 *&charIndices, 
    LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    le_int32 retCount = OpenTypeLayoutEngine::glyphProcessing(chars, offset, count, max, rightToLeft, featureTags, glyphs, charIndices, success);

    if (LE_FAILURE(success)) {
        return 0;
    }

    IndicReordering::adjustMPres(fMPreFixups, glyphs, charIndices);

    return retCount;
}

// Input: characters
// Output: characters, char indices, tags
// Returns: output character count
le_int32 IndicOpenTypeLayoutEngine::characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool /*rightToLeft*/,
        LEUnicode *&outChars, le_int32 *&charIndices, const LETag **&featureTags, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    le_int32 worstCase = count * IndicReordering::getWorstCaseExpansion(fScriptCode);

    outChars = LE_NEW_ARRAY(LEUnicode, worstCase);

    if (outChars == NULL) {
        success = LE_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    charIndices = LE_NEW_ARRAY(le_int32, worstCase);
    if (charIndices == NULL) {
        LE_DELETE_ARRAY(outChars);
        success = LE_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    featureTags = LE_NEW_ARRAY(const LETag *, worstCase);

    if (featureTags == NULL) {
        LE_DELETE_ARRAY(charIndices);
        LE_DELETE_ARRAY(outChars);
        success = LE_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    // NOTE: assumes this allocates featureTags...
    // (probably better than doing the worst case stuff here...)
    return IndicReordering::reorder(&chars[offset], count, fScriptCode, outChars, charIndices, featureTags, &fMPreFixups);
}

